package fr.upem.chatfusion.client;

import javax.naming.Context;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.logging.Logger;

public class Client {

    private static final Logger LOGGER = Logger.getLogger("FusionChat-Client");

    private final SocketChannel channel;
    private final Selector selector;
    private final InetSocketAddress serverAddress;
    private final String nickname;

    private ServerContext context;

    public Client(String nickname, InetSocketAddress inetSocketAddress) throws IOException {
        this.serverAddress = inetSocketAddress;
        this.nickname = nickname;
        this.channel = SocketChannel.open();
        this.selector = Selector.open();
    }

    public void launch() throws IOException {
        this.channel.configureBlocking(false);
        var key = this.channel.register(selector, SelectionKey.OP_CONNECT);
        this.context = new ServerContext(key);
        key.attach(context);
        this.channel.connect(serverAddress);

        //console.start();

        while (!Thread.interrupted()) {
            try {
                selector.select(k -> {
                    try {
                        treatKey(k);
                    } catch (InterruptedException e) {
                        LOGGER.info("Interrupted while treating key");
                    }
                });
                //processCommands();
            } catch (UncheckedIOException tunneled) {
                throw tunneled.getCause();
            }
        }
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

}
