package fr.upem.chatfusion.server;

import fr.upem.chatfusion.common.Channels;
import fr.upem.chatfusion.common.context.Context;
import fr.upem.chatfusion.common.packet.*;
import fr.upem.chatfusion.server.packet.ClientVisitor;
import fr.upem.chatfusion.server.packet.ServerVisitor;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;

public class Server {

    private static final Logger logger = Logger.getLogger(Server.class.getName());

    private final InetSocketAddress inetSocketAddress;
    private final ServerSocketChannel serverSocketChannel;
    private final Selector selector;
    private final BlockingQueue<Runnable> commandQueue;
    private final Thread console;
    private final HashMap<String, PeerContext> clients;
    private final HashMap<Integer, PeerContext> neighbors;
    private final int id;

    private boolean fusionLock = false;
    private HashSet<Integer> awaitingNeighbors = new HashSet<>();

    private PeerContext leader = null;

    public Server(int port, int id) throws IOException {
        this.inetSocketAddress = new InetSocketAddress(port);
        this.serverSocketChannel = ServerSocketChannel.open();
        this.serverSocketChannel.bind(inetSocketAddress);
        this.selector = Selector.open();
        this.commandQueue = new ArrayBlockingQueue<>(10);
        this.console = new Thread(new Console(this));
        this.console.setDaemon(true);
        this.clients = new HashMap<>();
        this.neighbors = new HashMap<>();
        this.id = id;
    }

    public void launch() throws IOException {
        serverSocketChannel.configureBlocking(false);
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

        console.start();
        System.out.println("Server " + id + " started");
        while (!Thread.interrupted() && serverSocketChannel.isOpen()) {
            try {
                //Helpers.printKeys(selector);
                selector.select(this::treatKey);
                processCommands();
            } catch (UncheckedIOException tunneled) {
                throw tunneled.getCause();
            } catch (InterruptedException e) {
                logger.info("Server interrupted");
            }
        }
        console.interrupt();
    }

    private void treatKey(SelectionKey key) {
        try {
            if (key.isValid() && key.isAcceptable()) {
                doAccept(key);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        try {
            if (key.isValid() && key.isReadable()) {
                ((Context) key.attachment()).doRead();
            } else if (key.isValid() && key.isWritable()) {
                ((Context) key.attachment()).doWrite();
            } else if (key.isValid() && key.isConnectable()) {
                ((Context) key.attachment()).doConnect();
            }
        } catch (IOException e) {
            logger.info("Connection closed");
            Channels.silentlyClose(key.channel());
        }
    }

    private void doAccept(SelectionKey key) throws IOException {
        var ssc = (ServerSocketChannel) key.channel();
        var sc = ssc.accept();
        if (sc == null) {
            logger.severe("Selector gave a bad hint");
            return;
        }
        sc.configureBlocking(false);
        var k = sc.register(selector, SelectionKey.OP_READ);
        var context = new PeerContext(this, k, (InetSocketAddress) sc.getRemoteAddress(), true);
        k.attach(context);
    }

    private void enqueueCommand(Runnable command) throws InterruptedException {
        commandQueue.put(command);
        if (!commandQueue.isEmpty()) {
            selector.wakeup();
        }
    }

    private void processCommands() throws InterruptedException {
        while (!commandQueue.isEmpty()) {
            commandQueue.take().run();
        }
    }

    public void authenticateGuest(String name, SelectionKey key) {
        Objects.requireNonNull(name);
        Objects.requireNonNull(key);
        var context = (PeerContext) key.attachment();
        if (clients.putIfAbsent(name, context) == null) {
            context.enqueuePacket(new AuthRsp(AuthRsp.OK, id));
            context.setVisitor(new ClientVisitor(this, context));
            context.setNickname(name);
            System.out.println("Guest " + name + " connected");
        }
    }

    public void disconnect(String nickname) {
        Objects.requireNonNull(nickname);
        if (clients.containsKey(nickname)) {
            clients.remove(nickname);
            System.out.println("Client " + nickname + " disconnected");
        }
    }

    public void dispatchPublicMessage(MsgPbl packet, boolean broadcast) {
        Objects.requireNonNull(packet);
        for (var context : clients.values()) {
            context.enqueuePacket(packet);
        }
        if (leader == null) {
            for (var entry : neighbors.entrySet()) {
                if (entry.getKey() != packet.serverId()) {
                    entry.getValue().enqueuePacket(packet);
                }
            }
        } else if (broadcast) {
            leader.enqueuePacket(packet);
        }
    }

    public void dispatchPrivateMessage(MsgPrv packet) {
        Objects.requireNonNull(packet);
        if (packet.dstServerId() == id) {
            var context = clients.get(packet.dstNickname());
            if (context != null) {
                context.enqueuePacket(packet);
            }
        } else {
            if (leader == null) {
                if (neighbors.containsKey(packet.dstServerId())) {
                    neighbors.get(packet.dstServerId()).enqueuePacket(packet);
                }
            } else {
                leader.enqueuePacket(packet);
            }
        }
    }

    public void dispatchFileChunk(FileChunk packet) {
        Objects.requireNonNull(packet);
        if (packet.dstServerId() == id) {
            var context = clients.get(packet.dstNickname());
            if (context != null) {
                context.enqueueFileChunk(packet);
            }
        } else {
            if (leader == null) {
                if (neighbors.containsKey(packet.dstServerId())) {
                    neighbors.get(packet.dstServerId()).enqueueFileChunk(packet);
                }
            } else {
                leader.enqueueFileChunk(packet);
            }
        }
    }

    private void initFusion(InetSocketAddress address) {
        Objects.requireNonNull(address);
        fusionLock = true;
        try {
            var socketChannel = SocketChannel.open();
            socketChannel.configureBlocking(false);
            var k = socketChannel.register(selector, SelectionKey.OP_CONNECT);
            var context = new PeerContext(this, k, address, false);
            k.attach(context);
            context.setVisitor(new ServerVisitor(this, context));
            context.enqueuePacket(new FusionInit(id, inetSocketAddress, new ArrayList<>(neighbors.keySet())));
            socketChannel.connect(address);
        } catch (IOException e) {
            System.out.println("Fail to connect to " + address);
        }
    }

    private void changeLeader(InetSocketAddress leader) {
        Objects.requireNonNull(leader);
        for (PeerContext context : neighbors.values()) {
            context.enqueuePacket(new FusionChangeLeader(leader));
            context.detach();
        }
    }

    public void initOutgoingFusion(InetSocketAddress address) throws InterruptedException {
        Objects.requireNonNull(address);
        enqueueCommand(() -> {
            if (fusionLock) {
                System.out.println("Fusion is locked. Try again later.");
                return;
            }
            if (leader != null) {
                System.out.println("Not leader, forwarding request...");
                leader.enqueuePacket(new FusionReq(address));
                return;
            }
            initFusion(address);
        });
    }

    public void initIncomingFusion(FusionInit packet, SelectionKey key) {
        Objects.requireNonNull(packet);
        Objects.requireNonNull(key);
        var context = ((PeerContext) key.attachment());
        if (fusionLock) {
            context.enqueuePacket(new FusionRsp(FusionRsp.ERROR));
            context.detach();
            return;
        }
        if (leader != null) {
            context.enqueuePacket(new FusionInitFwd(leader.getAddress()));
            return;
        }
        context.setVisitor(new ServerVisitor(this, context));
        for (var neighbor : packet.neighbors()) {
            if (neighbors.containsKey(neighbor)) {
                context.enqueuePacket(new FusionInitKo());
                System.out.println("Fusion with " + packet.serverId() + " rejected");
                return;
            }
        }
        if (neighbors.containsKey(packet.serverId())) {
            context.enqueuePacket(new FusionInitKo());
            System.out.println("Fusion with " + packet.serverId() + " rejected");
            return;
        }
        context.enqueuePacket(new FusionInitOk(id, inetSocketAddress, new ArrayList<>(neighbors.keySet())));
        if (packet.serverId() < id) {
            changeLeader(packet.address());
            neighbors.clear();
            leader = context;
            leader.setAddress(packet.address());
            System.out.println(packet.serverId() + " is the leader");
        } else {
            neighbors.put(packet.serverId(), context);
            awaitingNeighbors = new HashSet<>(packet.neighbors());
            leader = null;
            System.out.println("I'm the new leader");
        }
        releaseFusionLockIfPossible();
        System.out.println("Server " + packet.serverId() + " connected");
    }

    public void handleInitOk(FusionInitOk packet, PeerContext context) {
        Objects.requireNonNull(packet);
        Objects.requireNonNull(context);
        if (packet.serverId() < id) {
            changeLeader(packet.address());
            neighbors.clear();
            leader = context;
            leader.setAddress(packet.address());
            System.out.println(packet.serverId() + " is the new leader");
        } else {
            neighbors.put(packet.serverId(), context);
            awaitingNeighbors = new HashSet<>(packet.neighbors());
            leader = null;
            System.out.println("I'm the new leader");
        }
        releaseFusionLockIfPossible();
        System.out.println("Server " + packet.serverId() + " connected");
    }

    public void handleFusionReq(FusionReq packet, PeerContext context) {
        System.out.println("Received fusion request");
        if (fusionLock) {
            context.enqueuePacket(new FusionRsp(FusionRsp.ERROR));
            context.detach();
            return;
        }
        if (leader != null) {
            System.out.println("[SEVERE] Not leader, forwarding request...");
            leader.enqueuePacket(new FusionReq(packet.address()));
            return;
        }
        initFusion(packet.address());
    }

    public void switchLeader(InetSocketAddress address) {
        leader.detach();
        try {
            var socketChannel = SocketChannel.open();
            socketChannel.configureBlocking(false);
            var k = socketChannel.register(selector, SelectionKey.OP_CONNECT);
            var context = new PeerContext(this, k, address, false);
            k.attach(context);
            context.setVisitor(new ServerVisitor(this, context));
            context.enqueuePacket(new FusionAckLeader(id));
            socketChannel.connect(address);
            leader = context;
            System.out.println("Connecting to new leader " + address);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void handleFusionAckLeader(FusionAckLeader packet, SelectionKey key) {
        var context = (PeerContext) key.attachment();
        context.setVisitor(new ServerVisitor(this, context));
        neighbors.put(packet.serverId(), context);
        awaitingNeighbors.remove(packet.serverId());
        releaseFusionLockIfPossible();
    }

    public void releaseFusionLockIfPossible() {
        if (awaitingNeighbors.isEmpty()) {
            fusionLock = false;
        }
    }

    public HashMap<Integer, PeerContext> getNeighbors() {
        return neighbors;
    }
}
