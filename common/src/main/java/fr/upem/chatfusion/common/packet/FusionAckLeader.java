package fr.upem.chatfusion.common.packet;

import fr.upem.chatfusion.common.Buffers;
import fr.upem.chatfusion.common.reader.MultiPartReader;
import fr.upem.chatfusion.common.reader.Reader;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Objects;

import static java.nio.charset.StandardCharsets.UTF_8;

public record FusionAckLeader(String username) implements Packet {

    public FusionAckLeader {
        Objects.requireNonNull(username);
    }

    @Override
    public ByteBuffer toByteBuffer() {
        var usernameBytes = UTF_8.encode(username);
        var buffer = ByteBuffer.allocate(Byte.BYTES + Integer.BYTES + usernameBytes.remaining());

        buffer.put(OpCode.FUSION_ACKNOWLEDGE_LEADER.getCode());
        Buffers.putEncodedString(buffer, usernameBytes);
        return buffer;
    }

    @Override
    public void accept(PacketVisitor visitor) {
        visitor.visit(this);
    }

    public static Reader<FusionAckLeader> getReader() {
        return new AbstractPacketReader<>() {

            private String username;

            private final MultiPartReader<FusionAckLeader> reader = new MultiPartReader<>(List.of(
                    MultiPartReader.getString(s -> username = s)
            ), () -> new FusionAckLeader(username));

            @Override
            MultiPartReader<FusionAckLeader> reader() {
                return reader;
            }
        };
    }
}
