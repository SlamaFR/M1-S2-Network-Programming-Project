package fr.upem.chatfusion.server;

import fr.upem.chatfusion.common.Channels;
import fr.upem.chatfusion.common.Helpers;
import fr.upem.chatfusion.common.context.Context;
import fr.upem.chatfusion.common.packet.AuthRsp;
import fr.upem.chatfusion.common.packet.MsgPbl;
import fr.upem.chatfusion.common.packet.MsgPrv;
import fr.upem.chatfusion.common.packet.Packet;
import fr.upem.chatfusion.server.packet.ClientVisitor;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.util.HashMap;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Logger;

public class Server {

    private static final Logger logger = Logger.getLogger(Server.class.getName());

    private final ServerSocketChannel serverSocketChannel;
    private final Selector selector;
    private final BlockingQueue<Runnable> commandQueue;
    private final Thread console;
    private final HashMap<String, PeerContext> clients;
    private final int id;
    // private final HashSet<ServerContext> siblings;


    public Server(int port, int id) throws IOException {
        this.serverSocketChannel = ServerSocketChannel.open();
        this.serverSocketChannel.bind(new InetSocketAddress(port));
        this.selector = Selector.open();
        this.commandQueue = new ArrayBlockingQueue<>(10);
        this.console = new Thread(new Console(this));
        this.console.setDaemon(true);
        this.clients = new HashMap<>();
        this.id = id;
    }

    public void launch() throws IOException {
        serverSocketChannel.configureBlocking(false);
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
        console.start();
        System.out.println("Server ID#" + id + " started");
        while (!Thread.interrupted() && serverSocketChannel.isOpen()) {
            try {
                //Helpers.printKeys(selector);
                selector.select(this::treatKey);
                processCommands();
            } catch (UncheckedIOException tunneled) {
                throw tunneled.getCause();
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
        k.attach(new PeerContext(this, k));
    }

    private void processCommands() {
        // ouep
    }

    public void authenticateGuest(String name, SelectionKey key) {
        Objects.requireNonNull(name);
        Objects.requireNonNull(key);
        var context = (PeerContext) key.attachment();
        if (clients.putIfAbsent(name, context) == null) {
            context.enqueuePacket(new AuthRsp(AuthRsp.OK, id));
            context.setVisitor(new ClientVisitor(this));
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

    public void dispatchPublicMessage(MsgPbl packet) {
        Objects.requireNonNull(packet);
        for (var context : clients.values()) {
            context.enqueuePacket(packet);
        }
    }

    public void dispatchPrivateMessage(MsgPrv packet) {
        if (packet.dstServerId() == id) {
            var context = clients.get(packet.dstNickname());
            if (context != null) {
                context.enqueuePacket(packet);
            }
        } else {
            System.out.println("FOR ANOTHER SERVER");
        }
    }
}
