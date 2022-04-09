package fr.upem.chatfusion.common.packet;

import fr.upem.chatfusion.common.reader.MultiPartReader;
import fr.upem.chatfusion.common.reader.Reader;

import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Objects;

public record FusionInitFwd(InetSocketAddress address) implements Packet {

    public FusionInitFwd {
        Objects.requireNonNull(address);
    }

    @Override
    public ByteBuffer toByteBuffer() {
        var addressBytes = address.getAddress().getAddress();
        var addressPort = address.getPort();
        var buffer = ByteBuffer.allocate(Byte.BYTES + Integer.BYTES + addressBytes.length * Byte.BYTES + Integer.BYTES);

        buffer.put(OpCode.FUSION_INIT_FORWARD.getCode());
        buffer.putInt(addressBytes.length);
        buffer.put(addressBytes);
        buffer.putInt(addressPort);
        return buffer;
    }

    @Override
    public void accept(PacketVisitor visitor) {
        Objects.requireNonNull(visitor);
        visitor.visit(this);
    }

    public static Reader<FusionInitFwd> getReader() {
        return new AbstractPacketReader<>() {

            private byte[] bytes;
            private int port;

            private final MultiPartReader<FusionInitFwd> reader = new MultiPartReader<>(List.of(
                    MultiPartReader.getBytes(b -> bytes = b),
                    MultiPartReader.getInt(i -> port = i)
            ), () -> {
                try {
                    return new FusionInitFwd(new InetSocketAddress(InetAddress.getByAddress(bytes), port));
                } catch (UnknownHostException e) {
                    throw new UncheckedIOException(e);
                }
            });

            @Override
            MultiPartReader<FusionInitFwd> reader() {
                return reader;
            }
        };
    }
}
