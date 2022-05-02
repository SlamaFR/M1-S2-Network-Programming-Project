package fr.upem.chatfusion.common.packet;

import fr.upem.chatfusion.common.Buffers;
import fr.upem.chatfusion.common.reader.MultiPartReader;
import fr.upem.chatfusion.common.reader.Reader;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Objects;

import static java.nio.charset.StandardCharsets.UTF_8;

public record FusionAckLeader(int serverId) implements Packet {

    @Override
    public ByteBuffer toByteBuffer() {
        var buffer = ByteBuffer.allocate(Byte.BYTES + Integer.BYTES);

        buffer.put(OpCode.FUSION_ACKNOWLEDGE_LEADER.getCode());
        buffer.putInt(serverId);
        return buffer;
    }

    @Override
    public void accept(PacketVisitor visitor) {
        Objects.requireNonNull(visitor);
        visitor.visit(this);
    }

    public static Reader<FusionAckLeader> getReader() {
        return new AbstractPacketReader<>() {

            private int serverId;

            private final MultiPartReader<FusionAckLeader> reader = new MultiPartReader<>(List.of(
                    MultiPartReader.getInt(i -> serverId = i)
            ), () -> new FusionAckLeader(serverId));

            @Override
            MultiPartReader<FusionAckLeader> reader() {
                return reader;
            }
        };
    }
}
