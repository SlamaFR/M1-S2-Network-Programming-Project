package fr.upem.chatfusion.server;

import fr.upem.chatfusion.common.Channels;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
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

    public Server(int port, int id) throws IOException {
        this.id = id;
        this.serverSocketChannel = ServerSocketChannel.open();
        this.serverSocketChannel.bind(new InetSocketAddress(port));
        this.selector = Selector.open();
        this.commandQueue = new LinkedBlockingQueue<>();
        this.console = new Thread(new Console(this));
        this.console.setDaemon(true);
    }

    public void launch() throws IOException {
        serverSocketChannel.configureBlocking(false);
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
        console.start();
        while (!Thread.interrupted() && serverSocketChannel.isOpen()) {
            try {
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
            switch (commandQueue.poll().toUpperCase()) {
                case "STOP" -> {
                    logger.info("Stopping server...");
                    Channels.silentlyClose(serverSocketChannel);
                }
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
        k.attach(new ClientContext(k));
    }

}
