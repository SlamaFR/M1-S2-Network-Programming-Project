package fr.upem.chatfusion.server;

import fr.upem.chatfusion.common.Buffers;
import fr.upem.chatfusion.common.Channels;
import fr.upem.chatfusion.common.packet.Packet;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.ArrayDeque;
import java.util.logging.Logger;

public class ServerContext {

    private static final Logger LOGGER = Logger.getLogger(ServerContext.class.getName());

    private final int serverId;
    private final SelectionKey key;
    private final SocketChannel channel;
    private final ByteBuffer bufferIn;
    private final ByteBuffer bufferOut;
    private final ArrayDeque<ByteBuffer> queue;

    private boolean closed = false;

    public ServerContext(int serverId, SelectionKey key) {
        this.serverId = serverId;
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

    public void doConnect() throws IOException {
        if (!channel.finishConnect()) {
            return;
        }
        key.interestOps(SelectionKey.OP_WRITE);
    }

    /**
     * Adds a packet to the queue.
     * @param packet the packet to add
     */
    public void enqueuePacket(Packet packet) {
        queue.offer(packet.toByteBuffer().flip());
        processOut();
        updateInterestOps();
    }

    private void processIn() {
        bufferIn.flip();
        while (bufferIn.hasRemaining()) {
            try {
                var code = Packet.OpCode.fromCode(bufferIn.get());
                bufferIn.compact();
                System.out.println("Received packet: " + code);
                switch (code) {
                    case FUSION_INIT -> {
                        // TODO: Handle fusion init
                    }
                    default -> {
                        LOGGER.severe("OpCode not implemented: " + code);
                        closed = true;
                        return;
                    }
                }
                bufferIn.flip();
            } catch (IllegalArgumentException e) {
                LOGGER.severe("Unknown packet OpCode");
                closed = true;
            }
        }
        bufferIn.compact();
    }

    private void processOut() {
        while (bufferOut.hasRemaining() && !queue.isEmpty()) {
            var msg = queue.peek();
            if (!msg.hasRemaining()) {
                queue.pop();
                continue;
            }
            Buffers.tryPut(bufferOut, msg);
        }
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

    public int getServerId() {
        return serverId;
    }
}
