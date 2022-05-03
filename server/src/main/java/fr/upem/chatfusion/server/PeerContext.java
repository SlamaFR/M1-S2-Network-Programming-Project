package fr.upem.chatfusion.server;

import fr.upem.chatfusion.common.Buffers;
import fr.upem.chatfusion.common.context.AbstractContext;
import fr.upem.chatfusion.common.packet.PacketVisitor;
import fr.upem.chatfusion.common.reader.PacketReader;
import fr.upem.chatfusion.common.reader.Reader;
import fr.upem.chatfusion.server.packet.DefaultVisitor;

import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.util.logging.Logger;

public class PeerContext extends AbstractContext {

    private static final Logger logger = Logger.getLogger(PeerContext.class.getName());

    private final Server server;
    private final PacketReader packetReader;

    private InetSocketAddress address;
    private PacketVisitor visitor;
    private String nickname;

    public PeerContext(Server server, SelectionKey key, InetSocketAddress address, boolean incoming) {
        super(key, incoming);
        this.server = server;
        this.address = address;
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
                continue;
            }
            Buffers.tryPut(bufferOut, buffer);
        }
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
