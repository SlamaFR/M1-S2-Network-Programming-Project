package fr.upem.chatfusion.client;

import fr.upem.chatfusion.common.Buffers;
import fr.upem.chatfusion.common.context.AbstractContext;
import fr.upem.chatfusion.common.packet.FileChunk;
import fr.upem.chatfusion.common.reader.PacketReader;
import fr.upem.chatfusion.common.reader.Reader;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.util.ArrayDeque;
import java.util.Objects;
import java.util.Queue;
import java.util.logging.Logger;

public class ServerContext extends AbstractContext {

    private static final Logger logger = Logger.getLogger(ServerContext.class.getName());

    private final PacketReader packetReader;
    private final ServerVisitor serverVisitor;
    private final Queue<ByteBuffer> fileQueue;

    public ServerContext(Client client, SelectionKey key) {
        super(key, false);
        Objects.requireNonNull(client);
        this.packetReader = new PacketReader();
        this.serverVisitor = new ServerVisitor(client);
        this.fileQueue = new ArrayDeque<>();
    }

    @Override
    public void processIn() {
        try {
            while (bufferIn.hasRemaining()) {
                var status = packetReader.process(bufferIn);
                if (status != Reader.ProcessStatus.DONE) {
                    if (status == Reader.ProcessStatus.ERROR) {
                        System.out.println("AN ERROR OCCURED IN SERVER CONTEXT WITH READER");
                        closed = true;
                    }
                    break;
                }
                packetReader.get().accept(serverVisitor);
                packetReader.reset();
            }
        } catch (IllegalArgumentException e) {
            logger.severe("Unknown packet code");
            closed = true;
        }
    }

    @Override
    public void processOut() {
        while(bufferOut.hasRemaining() && (!queue.isEmpty() || !fileQueue.isEmpty())) {
            processQueue(queue);
            processQueue(fileQueue);
        }
    }

    public void enqueueFileChunk(FileChunk fileChunk) {
        Objects.requireNonNull(fileChunk);
        fileQueue.add(fileChunk.toByteBuffer().flip());
        processOut();
        updateInterestOps();
    }

    private void processQueue(Queue<ByteBuffer> queue) {
        if (queue.isEmpty()) {
            return;
        }
        var buffer = queue.peek();
        if (!buffer.hasRemaining()) {
            queue.poll();
            return;
        }
        Buffers.tryPut(bufferOut, buffer);
    }
}
