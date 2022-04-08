package fr.upem.chatfusion.common.packet;

import fr.upem.chatfusion.common.reader.MultiPartReader;
import fr.upem.chatfusion.common.reader.Reader;

import java.nio.ByteBuffer;
import java.util.List;

public record AuthRsp(byte code) implements Packet {

    public static final byte OK = 0x00;
    /* public static final byte NICKNAME_TAKEN = 0x01; */
    /* public static final byte BAD_PASSWORD = 0x02; */
    public static final byte NICKNAME_IN_USE = 0x03;

    @Override
    public ByteBuffer toByteBuffer() {
        var buffer = ByteBuffer.allocate(2 * Byte.BYTES);

        buffer.put(OpCode.AUTHENTICATION_RESPONSE.getCode());
        buffer.put(code);
        return buffer;
    }

    @Override
    public void accept(PacketVisitor visitor) {
        visitor.visit(this);
    }

    public static Reader<AuthRsp> getReader() {
        return new AbstractPacketReader<>() {

            private byte code;

            private final MultiPartReader<AuthRsp> reader = new MultiPartReader<>(List.of(
                    MultiPartReader.getByte(b -> code = b)
            ), () -> new AuthRsp(code));

            @Override
            MultiPartReader<AuthRsp> reader() {
                return reader;
            }
        };
    }
}
