package fr.upem.chatfusion.common.context;

import fr.upem.chatfusion.common.Channels;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.ArrayDeque;
import java.util.Objects;
import java.util.logging.Logger;

public abstract class AbstractContext implements Context {

    private static final Logger logger = Logger.getLogger(AbstractContext.class.getName());
    private static final int BUFFER_SIZE = 10_000;

    protected final SelectionKey key;
    protected final SocketChannel channel;
    protected final ByteBuffer bufferIn;
    protected final ByteBuffer bufferOut;
    protected final ArrayDeque<ByteBuffer> queue;

    protected boolean closed = false;

    public AbstractContext(SelectionKey key) {
        Objects.requireNonNull(key);
        this.key = key;
        this.channel = (SocketChannel) key.channel();
        this.bufferIn = ByteBuffer.allocate(BUFFER_SIZE);
        this.bufferOut = ByteBuffer.allocate(BUFFER_SIZE);
        this.queue = new ArrayDeque<>();
    }

    @Override
    public void doRead() throws IOException {
        var bytes = channel.read(bufferIn);
        if (bytes == 0) {
            logger.severe("Selector gave a bad hint");
            return;
        }
        if (bytes == -1) {
            closed = true;
        }
        processIn();
        updateInterestOps();
    }

    @Override
    public void doWrite() throws IOException {
        bufferOut.flip();
        if (closed && !bufferOut.hasRemaining()) {
            close();
            return;
        }
        var bytes = channel.write(bufferOut);
        bufferOut.compact();
        if (bytes == 0) {
            logger.severe("Selector gave a bad hint");
        }
        processOut();
        updateInterestOps();
    }

    @Override
    public void doConnect() throws IOException {
        if (!channel.finishConnect()) {
            return;
        }
        updateInterestOps();
    }

    @Override
    public void updateInterestOps() {
        var interestOps = 0;
        if (!closed && bufferIn.hasRemaining()) {
            interestOps |= SelectionKey.OP_READ;
        }
        if (bufferOut.position() != 0) {
            interestOps |= SelectionKey.OP_WRITE;
        }
        if (interestOps == 0) {
            close();
            return;
        }
        key.interestOps(interestOps);
    }

    @Override
    public void close() {
        Channels.silentlyClose(channel);
    }
}
