package fr.upem.chatfusion.client;

import fr.upem.chatfusion.common.Channels;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.logging.Logger;

public class ServerContext {

    private static final Logger LOGGER = Logger.getLogger(ServerContext.class.getName());
    private static final int BUFFER_SIZE = 10_000;

    private final SelectionKey key;
    private final SocketChannel channel;
    private final ByteBuffer bufferIn;
    private final ByteBuffer bufferOut;

    private boolean closed = false;

    public ServerContext(SelectionKey key) {
        this.key = key;
        this.channel = ((SocketChannel) key.channel());
        this.bufferIn = ByteBuffer.allocateDirect(BUFFER_SIZE);
        this.bufferOut = ByteBuffer.allocateDirect(BUFFER_SIZE);
    }

    public void doRead() throws IOException {
        var bytes = channel.read(bufferIn);
        if (bytes == 0) {
            LOGGER.severe("Selector gave a bad hint");
            return;
        }
        if (bytes == -1) {
            LOGGER.info("Connection closed by peer");
            closed = true;
        }
        processIn();
        updateInterestOps();
    }

    public void doWrite() throws IOException {
        bufferOut.flip();
        if (closed && !bufferOut.hasRemaining()) {
            Channels.silentlyClose(channel);
            return;
        }
        var bytes = channel.write(bufferOut);
        bufferOut.compact();
        if (bytes == 0) {
            LOGGER.severe("Selector gave a bad hint");
        }
        processOut();
        updateInterestOps();
    }

    public void doConnect() throws IOException {
        if (!channel.finishConnect()) {
            return;
        }
        key.interestOps(SelectionKey.OP_READ);
    }

    private void processIn() {
        // TODO
    }

    private void processOut() {
        // TODO
    }

    private void updateInterestOps() {
        var interestOps = 0;
        if (!closed && bufferIn.hasRemaining()) {
            interestOps |= SelectionKey.OP_READ;
        }
        if (!closed && bufferOut.position() != 0) {
            interestOps |= SelectionKey.OP_WRITE;
        }
        if (interestOps == 0) {
            Channels.silentlyClose(channel);
            return;
        }
        key.interestOps(interestOps);
    }

}
