package fr.upem.chatfusion.server;

import fr.upem.chatfusion.common.Channels;
import fr.upem.chatfusion.common.Helpers;
import fr.upem.chatfusion.common.packet.Packet;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Server {

    private static final Logger logger = Logger.getLogger("ChatFusion-Server");

    private final int id;

    private final ServerSocketChannel serverSocketChannel;
    private final Selector selector;
    private final BlockingQueue<String> commandQueue;
    private final Thread console;
    private final HashSet<ServerContext> siblings;
    private final HashMap<String, ClientContext> clients;

    private ServerContext leader = null;
    private boolean fusionLock = false;

    public Server(int port, int id) throws IOException {
        this.id = id;
        this.serverSocketChannel = ServerSocketChannel.open();
        this.serverSocketChannel.bind(new InetSocketAddress(port));
        this.selector = Selector.open();
        this.commandQueue = new LinkedBlockingQueue<>();
        this.console = new Thread(new Console(this));
        this.console.setDaemon(true);
        this.siblings = new HashSet<>();
        this.clients = new HashMap<>();
    }

    public void launch() throws IOException {
        serverSocketChannel.configureBlocking(false);
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
        console.start();
        System.out.println("Server ID#" + id + " started");
        while (!Thread.interrupted() && serverSocketChannel.isOpen()) {
            try {
                Helpers.printKeys(selector);
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
        } catch (IOException ioe) {
            throw new UncheckedIOException(ioe);
        }
        try {
            if (key.isValid() && key.isWritable()) {
                ((ClientContext) key.attachment()).doWrite();
            }
            if (key.isValid() && key.isReadable()) {
                ((ClientContext) key.attachment()).doRead();
            }
        } catch (IOException e) {
            logger.log(Level.INFO, "Connection closed with client", e);
            Channels.silentlyClose(key);
        }
    }

    /**
     * Sends instructions to the selector via a BlockingQueue and wakes it up.
     */
    public void sendCommand(String cmd) throws InterruptedException {
        commandQueue.put(cmd);
        if (!commandQueue.isEmpty()) {
            selector.wakeup();
        }
    }

    private void processCommands() {
        while (!commandQueue.isEmpty()) {
            var cmd = commandQueue.poll();
            if ("STOP".equalsIgnoreCase(cmd)) {
                logger.info("Stopping server...");
                Channels.silentlyClose(serverSocketChannel);
            } else if (cmd.toUpperCase().startsWith("FUSION")) {
                var split = cmd.split(" ");
                if (split.length != 3) {
                    System.out.println("Invalid syntax");
                    System.out.println("> FUSION <IP> <Port>");
                    continue;
                }
                var socket = new InetSocketAddress(split[1], Integer.parseInt(split[2]));
                System.out.println("Fusion with " + socket + "...");
                // TODO
            }
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
        k.attach(new ClientContext(this, k));
    }

    public boolean authenticateGuest(ClientContext client) {
        var e = clients.putIfAbsent(client.getNickname(), client) == null;
        return e;
    }

    public void disconnect(ClientContext client) {
        clients.remove(client.getNickname());
    }

    public void dispatchPacket(Packet packet) {
        for (var client : clients.values()) {
            client.enqueuePacket(packet);
        }
    }

    public void sendPacket(Packet packet, String nickname) {
        clients.get(nickname).enqueuePacket(packet);
    }

    public boolean isLeader() {
        return leader == null;
    }

}
