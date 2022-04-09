package fr.upem.chatfusion.common.packet;

import fr.upem.chatfusion.common.reader.MultiPartReader;
import fr.upem.chatfusion.common.reader.Reader;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Objects;

public record AuthRsp(byte code, int serverId) implements Packet {

    public static final byte OK = 0x00;
    /* public static final byte NICKNAME_TAKEN = 0x01; */
    /* public static final byte BAD_PASSWORD = 0x02; */
    public static final byte NICKNAME_IN_USE = 0x03;

    @Override
    public ByteBuffer toByteBuffer() {
        var buffer = ByteBuffer.allocate(2 * Byte.BYTES + Integer.BYTES);

        buffer.put(OpCode.AUTHENTICATION_RESPONSE.getCode());
        buffer.put(code);
        buffer.putInt(serverId);
        return buffer;
    }

    @Override
    public void accept(PacketVisitor visitor) {
        Objects.requireNonNull(visitor);
        visitor.visit(this);
    }

    public static Reader<AuthRsp> getReader() {
        return new AbstractPacketReader<>() {

            private byte code;
            private int serverId;

            private final MultiPartReader<AuthRsp> reader = new MultiPartReader<>(List.of(
                    MultiPartReader.getByte(b -> code = b),
                    MultiPartReader.getInt(i -> serverId = i)
            ), () -> new AuthRsp(code, serverId));

            @Override
            MultiPartReader<AuthRsp> reader() {
                return reader;
            }
        };
    }
}
