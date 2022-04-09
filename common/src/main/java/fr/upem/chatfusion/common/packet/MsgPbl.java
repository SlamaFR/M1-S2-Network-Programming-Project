package fr.upem.chatfusion.common.packet;

import fr.upem.chatfusion.common.Buffers;
import fr.upem.chatfusion.common.reader.MultiPartReader;
import fr.upem.chatfusion.common.reader.Reader;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Objects;

import static java.nio.charset.StandardCharsets.UTF_8;

public record MsgPbl(int serverId, String nickname, String message) implements Packet {

    public MsgPbl {
        Objects.requireNonNull(nickname);
        Objects.requireNonNull(message);
    }

    @Override
    public ByteBuffer toByteBuffer() {
        var nicknameBytes = UTF_8.encode(nickname);
        var messageBytes = UTF_8.encode(message);
        var buffer = ByteBuffer.allocate(Byte.BYTES + 3 * Integer.BYTES + nicknameBytes.remaining() + messageBytes.remaining());

        buffer.put(OpCode.PUBLIC_MESSAGE.getCode());
        buffer.putInt(serverId);
        Buffers.putEncodedString(buffer, nicknameBytes);
        Buffers.putEncodedString(buffer, messageBytes);
        return buffer;
    }

    @Override
    public void accept(PacketVisitor visitor) {
        Objects.requireNonNull(visitor);
        visitor.visit(this);
    }

    public static Reader<MsgPbl> getReader() {
        return new AbstractPacketReader<>() {

            private int serverId;
            private String username;
            private String message;

            private final MultiPartReader<MsgPbl> reader = new MultiPartReader<>(List.of(
                    MultiPartReader.getInt(i -> serverId = i),
                    MultiPartReader.getString(s -> username = s),
                    MultiPartReader.getString(s -> message = s)
            ), () -> new MsgPbl(serverId, username, message));

            @Override
            MultiPartReader<MsgPbl> reader() {
                return reader;
            }
        };
    }
}
