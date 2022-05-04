package fr.upem.chatfusion.server;

import fr.upem.chatfusion.common.context.AbstractContext;
import fr.upem.chatfusion.common.packet.FileChunk;
import fr.upem.chatfusion.common.packet.PacketVisitor;
import fr.upem.chatfusion.common.reader.PacketReader;
import fr.upem.chatfusion.common.reader.Reader;
import fr.upem.chatfusion.server.packet.DefaultVisitor;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.util.ArrayDeque;
import java.util.Objects;
import java.util.Queue;
import java.util.logging.Logger;

public class PeerContext extends AbstractContext {

    private static final Logger logger = Logger.getLogger(PeerContext.class.getName());

    private final Server server;
    private final PacketReader packetReader;
    private final Queue<ByteBuffer> fileQueue;

    private InetSocketAddress address;
    private PacketVisitor visitor;
    private String nickname;


    public PeerContext(Server server, SelectionKey key, InetSocketAddress address, boolean incoming) {
        super(key, incoming);
        this.server = server;
        this.address = address;
        this.packetReader = new PacketReader();
        this.fileQueue = new ArrayDeque<>();
        this.visitor = new DefaultVisitor(server, key);
    }

    @Override
    public void processIn() {
        try {
            while (bufferIn.position() >= 0) {
                var status = packetReader.process(bufferIn);
                if (status != Reader.ProcessStatus.DONE) {
                    if (status == Reader.ProcessStatus.ERROR) {
                        closed = true;
                    }
                    break;
                }
                packetReader.get().accept(visitor);
                packetReader.reset();
            }
        } catch (IllegalArgumentException e) {
            logger.severe("Unknown packet code");
            closed = true;
        } catch (UnsupportedOperationException e) {
            logger.severe("Received unsupported packet in this context");
            closed = true;
        }
    }

    @Override
    public void processOut() {
        while(bufferOut.hasRemaining() && (!queue.isEmpty() || !fileQueue.isEmpty())) {
            if (!processQueue(queue) && !processQueue(fileQueue)){
                break;
            }
        }
    }


    private boolean processQueue(Queue<ByteBuffer> queue) {
        if (queue.isEmpty()) {
            return false;
        }
        var buffer = queue.peek();
        if (bufferOut.remaining() >= buffer.remaining()) {
            bufferOut.put(buffer);
            queue.poll();
            return true;
        }
        return false;
    }

    public void enqueueFileChunk(FileChunk fileChunk) {
        Objects.requireNonNull(fileChunk);
        fileQueue.add(fileChunk.toByteBuffer().flip());
        processOut();
        updateInterestOps();
    }

    public void setVisitor(PacketVisitor visitor) {
        this.visitor = visitor;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public InetSocketAddress getAddress() {
        return address;
    }

    public void setAddress(InetSocketAddress address) {
        this.address = address;
    }

    @Override
    public void close() {
        super.close();
        if (nickname != null) {
            server.disconnect(nickname);
        }
    }

    public void detach() {
        closed = true;
    }
}
