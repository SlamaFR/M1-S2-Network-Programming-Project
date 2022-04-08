package fr.upem.chatfusion.common.packet;

import fr.upem.chatfusion.common.Buffers;
import fr.upem.chatfusion.common.reader.MultiPartReader;
import fr.upem.chatfusion.common.reader.Reader;
import fr.upem.chatfusion.common.reader.StringReader;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Objects;

import static java.nio.charset.StandardCharsets.UTF_8;

public record AuthGst(String username) implements Packet {

    public AuthGst {
        Objects.requireNonNull(username);
    }

    @Override
    public ByteBuffer toByteBuffer() {
        var usernameBytes = UTF_8.encode(username);
        var buffer = ByteBuffer.allocate(Byte.BYTES + Integer.BYTES + usernameBytes.remaining());

        buffer.put(OpCode.AUTHENTICATION_GUEST.getCode());
        Buffers.putEncodedString(buffer, usernameBytes);
        return buffer;
    }

    @Override
    public void accept(PacketVisitor visitor) {
        visitor.visit(this);
    }

    public static Reader<AuthGst> getReader() {
        return new AbstractPacketReader<>() {

            private String username;

            private final MultiPartReader<AuthGst> reader = new MultiPartReader<>(List.of(
                    MultiPartReader.getString(s -> username = s)
            ), () -> new AuthGst(username));

            @Override
            MultiPartReader<AuthGst> reader() {
                return reader;
            }
        };
    }
}
