package fr.upem.chatfusion.client;

import fr.upem.chatfusion.common.Helpers;
import fr.upem.chatfusion.common.packet.OutgoingPublicMessage;

import javax.naming.Context;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;

public class Client {

    private static final Logger LOGGER = Logger.getLogger("FusionChat-Client");

    private final SocketChannel channel;
    private final Selector selector;
    private final InetSocketAddress serverAddress;
    private final String nickname;
    private final BlockingQueue<String> commandQueue;
    private final Thread console;

    private ServerContext context;

    public Client(String nickname, InetSocketAddress inetSocketAddress) throws IOException {
        this.serverAddress = inetSocketAddress;
        this.nickname = nickname;
        this.channel = SocketChannel.open();
        this.selector = Selector.open();
        this.commandQueue = new LinkedBlockingQueue<>();
        this.console = new Thread(new Console(this));
        this.console.setDaemon(true);
    }

    public void launch() throws IOException {
        this.channel.configureBlocking(false);
        var key = this.channel.register(selector, SelectionKey.OP_CONNECT);
        this.context = new ServerContext(key);
        key.attach(context);
        this.channel.connect(serverAddress);

        console.start();

        while (!Thread.interrupted()) {
            try {
                selector.select(k -> {
                    try {
                        treatKey(k);
                    } catch (InterruptedException e) {
                        LOGGER.info("Interrupted while treating key");
                    }
                });
                processCommands();
            } catch (UncheckedIOException tunneled) {
                throw tunneled.getCause();
            }
        }
        console.interrupt();
    }

    private void treatKey(SelectionKey key) throws InterruptedException {
        try {
            if (key.isValid() && key.isConnectable()) {
                this.context.doConnect();
            }
            if (key.isValid() && key.isWritable()) {
                this.context.doWrite();
            }
            if (key.isValid() && key.isReadable()) {
                this.context.doRead();
            }
        } catch (IOException ioe) {
            throw new UncheckedIOException(ioe);
        }
    }

    public void sendCommand(String cmd) throws InterruptedException {
        commandQueue.put(cmd);
        if (!commandQueue.isEmpty()) {
            selector.wakeup();
        }
    }

    private void processCommands() {
        while (!commandQueue.isEmpty()) {
            String cmd = commandQueue.poll();
            if (cmd.startsWith("/")) {
                // Private message
            } else if (cmd.startsWith("@")) {
                // File transfer
            } else {
                // Public message
                var packet = new OutgoingPublicMessage(cmd);
                context.enqueue(packet);
            }
        }
    }

}
