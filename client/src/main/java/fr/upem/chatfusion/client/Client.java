package fr.upem.chatfusion.client;

import fr.upem.chatfusion.common.Helpers;

import fr.upem.chatfusion.common.Helpers;
import fr.upem.chatfusion.common.frame.PrivateMessageFrame;
import fr.upem.chatfusion.common.packet.OutgoingPrivateMessage;
import fr.upem.chatfusion.common.packet.OutgoingPublicMessage;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.file.Path;
import java.util.Arrays;
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
    private final Path basePath;

    private ServerContext context;


    public Client(String nickname, InetSocketAddress inetSocketAddress, Path path) throws IOException {
        this.serverAddress = inetSocketAddress;
        this.nickname = nickname;
        this.channel = SocketChannel.open();
        this.selector = Selector.open();
        this.commandQueue = new LinkedBlockingQueue<>();
        this.console = new Thread(new Console(this));
        this.console.setDaemon(true);
        this.basePath = path;
    }

    public void launch() throws IOException {
        this.channel.configureBlocking(false);
        var key = this.channel.register(selector, SelectionKey.OP_CONNECT);
        this.context = new ServerContext(this, key);
        key.attach(context);
        this.channel.connect(serverAddress);

        console.start();

        while (!Thread.interrupted()) {
            try {
                Helpers.printKeys(selector);
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
            if (cmd.startsWith("@")) {
                cmd = cmd.substring(1);
                var split = cmd.split(" ");
                if (split.length < 2) {
                    System.out.println("Usage: @<Nickname>:<Server ID> <Message>");
                    continue;
                }
                var expeditionData = split[0].split(":");
                var recipient = expeditionData[0];
                var serverId = Integer.parseInt(expeditionData[1]);
                var message = String.join(" ", Arrays.copyOfRange(split, 1, split.length));
                var packet = new OutgoingPrivateMessage(serverId, recipient, message);
                context.enqueue(packet);
            } else if (cmd.startsWith("/")) {
                cmd = cmd.substring(1);
                var split = cmd.split(" ");
                if (split.length < 2) {
                    System.out.println("Usage: @<Nickname>:<Server ID> <Message>");
                    continue;
                }
                var expeditionData = split[0].split(":");
                var recipient = expeditionData[0];
                var serverId = Integer.parseInt(expeditionData[1]);
                var filepath = String.join(" ", Arrays.copyOfRange(split, 1, split.length));
                try {
                    new FileSender(context, serverId, recipient, Path.of(basePath + "/" + filepath)).sendAsync();
                } catch (IOException e) {
                    System.out.println("chelou");
                    System.out.println(e.getMessage());
                }

            } else {
                // Public message
                var packet = new OutgoingPublicMessage(cmd);
                context.enqueue(packet);
            }
        }
    }

    public String getNickname() {
        return nickname;
    }

    public void wakeup() {
        selector.wakeup();
    }

    public Path getBasePath() {
        return basePath;
    }
}
