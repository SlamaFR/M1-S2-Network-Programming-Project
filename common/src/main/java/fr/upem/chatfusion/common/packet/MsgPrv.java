package fr.upem.chatfusion.common.packet;

import fr.upem.chatfusion.common.Buffers;
import fr.upem.chatfusion.common.reader.MultiPartReader;
import fr.upem.chatfusion.common.reader.Reader;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Objects;

import static java.nio.charset.StandardCharsets.UTF_8;

public record MsgPrv(int srcServerId, String srcNickname, int dstServerId, String dstNickname, String message) implements Packet {

    public MsgPrv {
        Objects.requireNonNull(srcNickname);
        Objects.requireNonNull(dstNickname);
        Objects.requireNonNull(message);
    }

    @Override
    public ByteBuffer toByteBuffer() {
        var srcNicknameBytes = UTF_8.encode(srcNickname);
        var dstNicknameBytes = UTF_8.encode(dstNickname);
        var messageBytes = UTF_8.encode(message);
        var buffer = ByteBuffer.allocate(Byte.BYTES + 5 * Integer.BYTES + srcNicknameBytes.remaining() + dstNicknameBytes.remaining() + messageBytes.remaining());

        buffer.put(OpCode.PRIVATE_MESSAGE.getCode());
        buffer.putInt(srcServerId);
        Buffers.putEncodedString(buffer, srcNicknameBytes);
        buffer.putInt(dstServerId);
        Buffers.putEncodedString(buffer, dstNicknameBytes);
        Buffers.putEncodedString(buffer, messageBytes);
        return buffer;
    }

    @Override
    public void accept(PacketVisitor visitor) {
        Objects.requireNonNull(visitor);
        visitor.visit(this);
    }

    public static Reader<MsgPrv> getReader() {
        return new AbstractPacketReader<>() {

            private int srcServerId;
            private String srcNickname;
            private int dstServerId;
            private String dstNickname;
            private String message;

            private final MultiPartReader<MsgPrv> reader = new MultiPartReader<>(List.of(
                    MultiPartReader.getInt(i -> srcServerId = i),
                    MultiPartReader.getString(s -> srcNickname = s),
                    MultiPartReader.getInt(i -> dstServerId = i),
                    MultiPartReader.getString(s -> dstNickname = s),
                    MultiPartReader.getString(s -> message = s)
            ), () -> new MsgPrv(srcServerId, srcNickname, dstServerId, dstNickname, message));

            @Override
            MultiPartReader<MsgPrv> reader() {
                return reader;
            }
        };
    }
}
