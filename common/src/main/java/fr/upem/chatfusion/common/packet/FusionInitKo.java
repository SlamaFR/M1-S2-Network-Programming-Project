package fr.upem.chatfusion.common.packet;

import fr.upem.chatfusion.common.reader.MultiPartReader;
import fr.upem.chatfusion.common.reader.Reader;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Objects;

public record FusionInitKo() implements Packet {

    @Override
    public ByteBuffer toByteBuffer() {
        var buffer = ByteBuffer.allocate(Byte.BYTES);

        buffer.put(OpCode.FUSION_INIT_KO.getCode());
        return buffer;
    }

    @Override
    public void accept(PacketVisitor visitor) {
        Objects.requireNonNull(visitor);
        visitor.visit(this);
    }

    public static Reader<FusionInitKo> getReader() {
        return new AbstractPacketReader<>() {

            private final MultiPartReader<FusionInitKo> reader = new MultiPartReader<>(List.of(), FusionInitKo::new);

            @Override
            MultiPartReader<FusionInitKo> reader() {
                return reader;
            }
        };
    }
}
