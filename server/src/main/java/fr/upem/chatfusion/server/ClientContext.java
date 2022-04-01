package fr.upem.chatfusion.server;

import fr.upem.chatfusion.common.Channels;
import fr.upem.chatfusion.common.packet.Packet;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.logging.Logger;

public class ClientContext {

    private static final Logger LOGGER = Logger.getLogger(ClientContext.class.getName());

    private final SelectionKey key;
    private final SocketChannel channel;
    private final ByteBuffer bufferIn;
    private final ByteBuffer bufferOut;
    private final Queue<Packet> queue;

    private boolean closed = false;

    public ClientContext(SelectionKey key) {
        this.key = key;
        this.channel = (SocketChannel) key.channel();
        this.bufferIn = ByteBuffer.allocate(1_024);
        this.bufferOut = ByteBuffer.allocate(1_024);
        this.queue = new ArrayDeque<>();
    }

    /**
     * Performs a read on the channel.
     * <p>
     * <b>Notice:</b> All buffers should be in write mode.
     *
     * @throws IOException if an I/O error occurs
     */
    public void doRead() throws IOException {
        var bytes = channel.read(bufferIn);
        if (bytes == 0) {
            LOGGER.severe("Selector gave a bad hint");
            return;
        }
        if (bytes == -1) {
            closed = true;
        }
        processIn();
        updateInterestOps();
    }

    /**
     * Performs a writing on the channel.
     * <p>
     * <b>Notice:</b> All buffers should be in write mode.
     *
     * @throws IOException if an I/O error occurs
     */
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

    private void processIn() {
        // TODO
    }

    private void processOut() {
        // TODO
    }

    /**
     * Updates the interest ops of the channel based on the state
     * of the buffers.
     */
    private void updateInterestOps() {
        var interestOps = 0;
        if (!closed && bufferIn.hasRemaining()) {
            interestOps |= SelectionKey.OP_READ;
        }
        if (bufferOut.position() != 0) {
            interestOps |= SelectionKey.OP_WRITE;
        }
        if (interestOps == 0) {
            Channels.silentlyClose(channel);
            return;
        }
        key.interestOps(interestOps);
    }
}
