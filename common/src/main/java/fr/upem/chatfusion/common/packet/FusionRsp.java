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

public record FusionRsp(byte status) implements Packet {

    @Override
    public ByteBuffer toByteBuffer() {
        var buffer = ByteBuffer.allocate(2 * Byte.BYTES);

        buffer.put(OpCode.FUSION_REQUEST_RESPONSE.getCode());
        buffer.put(status);
        return buffer;
    }

    @Override
    public void accept(PacketVisitor visitor) {
        Objects.requireNonNull(visitor);
        visitor.visit(this);
    }

    public static Reader<FusionRsp> getReader() {
        return new AbstractPacketReader<>() {

            private byte status;

            private final MultiPartReader<FusionRsp> reader = new MultiPartReader<>(List.of(
                    MultiPartReader.getByte(b -> status = b)
            ), () -> new FusionRsp(status));

            @Override
            MultiPartReader<FusionRsp> reader() {
                return reader;
            }
        };
    }
}
