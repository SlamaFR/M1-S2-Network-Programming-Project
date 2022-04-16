package fr.upem.chatfusion.server;

import fr.upem.chatfusion.common.Buffers;
import fr.upem.chatfusion.common.context.AbstractContext;
import fr.upem.chatfusion.common.packet.FileChunk;
import fr.upem.chatfusion.common.packet.PacketVisitor;
import fr.upem.chatfusion.common.reader.PacketReader;
import fr.upem.chatfusion.common.reader.Reader;
import fr.upem.chatfusion.server.packet.DefaultVisitor;

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

    private PacketVisitor visitor;
    private String nickname;

    public PeerContext(Server server, SelectionKey key) {
        super(key, true);
        this.server = server;
        this.packetReader = new PacketReader();
        this.visitor = new DefaultVisitor(server, key);
        this.fileQueue = new ArrayDeque<>();
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
            System.out.println("Unknown packet code");
            closed = true;
        } catch (UnsupportedOperationException e) {
            System.out.println("Received unsupported packet in this context");
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

    public void setVisitor(PacketVisitor visitor) {
        this.visitor = visitor;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public void enqueueFileChunk(FileChunk fileChunk) {
        Objects.requireNonNull(fileChunk);
        fileQueue.add(fileChunk.toByteBuffer().flip());
        processOut();
        updateInterestOps();
    }

    @Override
    public void close() {
        super.close();
        if (nickname != null) {
            server.disconnect(nickname);
        }
    }


}
