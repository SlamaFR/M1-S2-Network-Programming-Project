package fr.upem.chatfusion.server;

import fr.upem.chatfusion.common.Buffers;
import fr.upem.chatfusion.common.Channels;
import fr.upem.chatfusion.common.packet.AuthenticationGuest;
import fr.upem.chatfusion.common.packet.Packet;
import fr.upem.chatfusion.common.reader.AuthGuestReader;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.ArrayDeque;
import java.util.logging.Logger;

public class ClientContext {

    private static final Logger LOGGER = Logger.getLogger(ClientContext.class.getName());

    private final Server server;
    private final SelectionKey key;
    private final SocketChannel channel;
    private final ByteBuffer bufferIn;
    private final ByteBuffer bufferOut;
    private final ArrayDeque<ByteBuffer> queue;

    private final AuthGuestReader authGuestReader;

    private boolean authenticated = false;
    private String nickname;
    private boolean closed = false;

    public ClientContext(Server server, SelectionKey key) {
        this.server = server;
        this.key = key;
        this.channel = (SocketChannel) key.channel();
        this.bufferIn = ByteBuffer.allocate(1_024);
        this.bufferOut = ByteBuffer.allocate(1_024);
        this.queue = new ArrayDeque<>();

        this.authGuestReader = new AuthGuestReader();
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
        while (bufferIn.hasRemaining()) {
            try {
                var code = Packet.OpCode.fromCode(bufferIn.get());
                switch (code) {
                    case AUTHENTICATION_GUEST -> {
                        LOGGER.info("authentication");
                        if (authenticated) {
                            // TODO: already authenticated
                            System.out.println("Already authenticated");
                            continue;
                        }
                        if (!ReaderHandler.handlePacketReader(authGuestReader, bufferIn)) {
                            return;
                        }
                        handleAuthenticationGuest(authGuestReader.get());
                        authGuestReader.reset();
                    }
                    case INCOMING_PUBLIC_MESSAGE -> {
                        // TODO
                    }
                    default -> {
                        LOGGER.severe("OpCode not implemented: " + code);
                        closed = true;
                    }
                }
            } catch (IllegalArgumentException e) {
                LOGGER.severe("Unknown packet OpCode");
                closed = true;
            }
        }
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

    private void handleAuthenticationGuest(AuthenticationGuest packet) {
        this.nickname = packet.nickname();
        this.authenticated = server.authenticateGuest(this);
        if (!authenticated) {
            System.out.println("Could not authenticate guest");
            // TODO: send error packet
        } else {
            System.out.println("Guest authenticated");
            // TODO: send success packet
        }
    }

    public String getNickname() {
        return nickname;
    }
}
