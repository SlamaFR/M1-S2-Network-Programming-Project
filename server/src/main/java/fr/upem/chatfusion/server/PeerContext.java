package fr.upem.chatfusion.server;

import fr.upem.chatfusion.common.Buffers;
import fr.upem.chatfusion.common.context.AbstractContext;
import fr.upem.chatfusion.common.packet.PacketVisitor;
import fr.upem.chatfusion.common.reader.PacketReader;
import fr.upem.chatfusion.common.reader.Reader;
import fr.upem.chatfusion.server.packet.DefaultVisitor;

import java.nio.channels.SelectionKey;
import java.util.logging.Logger;

public class PeerContext extends AbstractContext {

    private static final Logger logger = Logger.getLogger(PeerContext.class.getName());

    private final Server server;
    private final PacketReader packetReader;

    private PacketVisitor visitor;
    private String nickname;

    public PeerContext(Server server, SelectionKey key) {
        super(key, true);
        this.server = server;
        this.packetReader = new PacketReader();
        this.visitor = new DefaultVisitor(server, key);
    }

    @Override
    public void processIn() {
        try {
            while (bufferIn.hasRemaining()) {
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
        while(bufferOut.hasRemaining() && !queue.isEmpty()) {
            var buffer = queue.peek();
            if (!buffer.hasRemaining()) {
                queue.poll();
                return;
            }
            Buffers.tryPut(bufferOut, buffer);
        }
    }

    public void setVisitor(PacketVisitor visitor) {
        this.visitor = visitor;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    @Override
    public void close() {
        super.close();
        if (nickname != null) {
            server.disconnect(nickname);
        }
    }
}
