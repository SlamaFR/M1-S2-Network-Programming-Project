package fr.upem.chatfusion.client;

import fr.upem.chatfusion.common.Buffers;
import fr.upem.chatfusion.common.Channels;
import fr.upem.chatfusion.common.packet.AuthenticationGuest;
import fr.upem.chatfusion.common.packet.AuthenticationGuestResponse;
import fr.upem.chatfusion.common.packet.IncomingPublicMessage;
import fr.upem.chatfusion.common.packet.Packet;
import fr.upem.chatfusion.common.reader.AuthGuestResponseReader;
import fr.upem.chatfusion.common.reader.InPublicMessageReader;
import fr.upem.chatfusion.common.reader.ReaderHandler;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.logging.Logger;

public class ServerContext {

    private static final Logger LOGGER = Logger.getLogger(ServerContext.class.getName());
    private static final int BUFFER_SIZE = 10_000;

    private final Client client;
    private final SelectionKey key;
    private final SocketChannel channel;
    private final ByteBuffer bufferIn;
    private final ByteBuffer bufferOut;
    private final ArrayDeque<ByteBuffer> queue = new ArrayDeque<>();

    private final InPublicMessageReader inPublicMessageReader;
    private final AuthGuestResponseReader authGuestResponseReader;

    private boolean closed = false;
    private boolean isConnected = false;

    public ServerContext(Client client, SelectionKey key) {
        this.client = client;
        this.key = key;
        this.channel = ((SocketChannel) key.channel());
        this.bufferIn = ByteBuffer.allocateDirect(BUFFER_SIZE);
        this.bufferOut = ByteBuffer.allocateDirect(BUFFER_SIZE);
        this.inPublicMessageReader = new InPublicMessageReader();
        this.authGuestResponseReader = new AuthGuestResponseReader();
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
        key.interestOps(SelectionKey.OP_WRITE);
        authenticate();
    }

    public void enqueue(Packet packet) {
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
                    case AUTHENTICATION_RESPONSE -> {
                        if(isConnected) {
                            System.out.println("Already connected. This should never happen");
                            return;
                        }
                        if (!ReaderHandler.handlePacketReader(authGuestResponseReader, bufferIn)){
                            return ;
                        }
                        var response = authGuestResponseReader.get();
                        if (response.code() == AuthenticationGuestResponse.AuthGuestResp.AUTHENTICATION_GUEST_SUCCESS) {
                            System.out.println("WELCOME " + client.getNickname() + " ON THE CHAT FUSION SERVER !");
                            isConnected = true;
                        }
                        else {
                            // TODO manage different AuthGuestRespCode when user connexion with pwd will be implemented
                            System.out.println("Sorry " + client.getNickname() +" someone with same name is already present.\nTry again with another login.");
                            closed = true;
                            return;
                        }
                    }
                    case INCOMING_PUBLIC_MESSAGE -> {
                        if (!ReaderHandler.handlePacketReader(inPublicMessageReader, bufferIn)) {
                            return;
                        }
                        var message = inPublicMessageReader.get();
                        System.out.println(message);
                        inPublicMessageReader.reset();
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
            Thread.currentThread().interrupt();
            return;
        }
        key.interestOps(interestOps);
    }

    private void authenticate() {
        enqueue(new AuthenticationGuest(client.getNickname()));
    }

}
