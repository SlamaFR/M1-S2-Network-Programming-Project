package fr.upem.chatfusion.client;

import fr.upem.chatfusion.common.Channels;
import fr.upem.chatfusion.common.Helpers;
import fr.upem.chatfusion.common.packet.AuthGst;
import fr.upem.chatfusion.common.packet.MsgPbl;
import fr.upem.chatfusion.common.packet.MsgPrv;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.logging.Logger;

public class Client {

    private static final Logger logger = Logger.getLogger("ChatFusion-Client");

    private final InetSocketAddress serverAddress;
    private final SocketChannel channel;
    private final Selector selector;
    private final String nickname;
    private final Path basePath;
    private final Thread console;
    private final ArrayBlockingQueue<Runnable> commands;
    private int transferIDCounter;

    private ServerContext uniqueContext;
    private int serverId;

    public Client(InetSocketAddress serverAddress, String nickname, Path basePath) throws IOException {
        this.serverAddress = serverAddress;
        this.channel = SocketChannel.open();
        this.selector = Selector.open();
        this.nickname = nickname;
        this.basePath = basePath;
        this.console = new Thread(new Console(this));
        this.commands = new ArrayBlockingQueue<>(10);
        this.transferIDCounter = 0;
    }

    public void launch() throws IOException {
        channel.configureBlocking(false);
        var key = channel.register(selector, SelectionKey.OP_CONNECT);
        uniqueContext = new ServerContext(this, key);
        channel.connect(serverAddress);

        console.setDaemon(true);
        // Sending authentication packet
        uniqueContext.enqueuePacket(new AuthGst(nickname));
        while (!Thread.interrupted()) {
            try {
                Helpers.printKeys(selector);
                selector.select(this::treatKey);
                processCommands();
            } catch (UncheckedIOException tunneled) {
                throw tunneled.getCause();
            } catch (InterruptedException e) {
                logger.info("Client interrupted");
            } catch (ClosedSelectorException e) {
                logger.info("Client closed");
                break;
            }
        }
        console.interrupt();
    }

    private void treatKey(SelectionKey key) {
        try {
            if (key.isValid() && key.isConnectable()) {
                uniqueContext.doConnect();
            }
            if (key.isValid() && key.isReadable()) {
                uniqueContext.doRead();
            }
            if (key.isValid() && key.isWritable()) {
                uniqueContext.doWrite();
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void enqueueCommand(Runnable command) throws InterruptedException {
        commands.put(command);
        if (!commands.isEmpty()) {
            selector.wakeup();
        }
    }

    private void processCommands() throws InterruptedException {
        while (!commands.isEmpty()) {
            commands.take().run();
        }
    }

    public void sendPublicMessage(String message) throws InterruptedException {
        Objects.requireNonNull(message);
        enqueueCommand(() -> uniqueContext.enqueuePacket(new MsgPbl(this.serverId, this.nickname, message)));
    }

    public void sendPrivateMessage(int serverId, String nickname, String message) throws InterruptedException {
        Objects.requireNonNull(nickname);
        Objects.requireNonNull(message);
        enqueueCommand(() -> uniqueContext.enqueuePacket(new MsgPrv(this.serverId, this.nickname, serverId, nickname, message)));
    }

    public void sendFile(int serverId, String nickname, String filePath) throws InterruptedException {
        Objects.requireNonNull(nickname);
        Objects.requireNonNull(filePath);
        enqueueCommand(() -> {
            try {
                new FileSender(uniqueContext, selector, this.serverId, serverId, this.nickname, transferIDCounter, nickname, Path.of(basePath + "/" + filePath)).send();
                transferIDCounter++;
            } catch (IOException e) {
                System.err.println(e.getMessage());
                return ;
            }
        });
    }

    public void shutdown() {
        Channels.silentlyClose(selector);
    }

    public void confirmAuthentication(int serverId) {
        this.serverId = serverId;
        console.start();
        System.out.println("Welcome to ChatFusion, " + nickname + "!");
    }
}
