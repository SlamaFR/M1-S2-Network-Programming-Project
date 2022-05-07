package fr.upem.chatfusion.common.packet;

import fr.upem.chatfusion.common.Buffers;
import fr.upem.chatfusion.common.reader.MultiPartReader;
import fr.upem.chatfusion.common.reader.Reader;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Objects;

import static java.nio.charset.StandardCharsets.UTF_8;

public record AuthUsr(String username, String password) implements Packet {

    public AuthUsr {
        Objects.requireNonNull(username);
        Objects.requireNonNull(password);
    }

    @Override
    public ByteBuffer toByteBuffer() {
        var usernameBytes = UTF_8.encode(username);
        var passwordBytes = UTF_8.encode(password);
        var buffer = ByteBuffer.allocate(Byte.BYTES + 2 * Integer.BYTES + usernameBytes.remaining() + passwordBytes.remaining());

        buffer.put(OpCode.AUTHENTICATION_USER.getCode());
        Buffers.putEncodedString(buffer, usernameBytes);
        Buffers.putEncodedString(buffer, passwordBytes);
        return buffer;
    }

    @Override
    public void accept(PacketVisitor visitor) {
        Objects.requireNonNull(visitor);
        visitor.visit(this);
    }

    public static Reader<AuthUsr> getReader() {
        return new AbstractPacketReader<>() {

            private String username;
            private String password;

            private final MultiPartReader<AuthUsr> reader = new MultiPartReader<>(List.of(
                    MultiPartReader.getString(s -> username = s),
                    MultiPartReader.getString(s -> password = s)
            ), () -> new AuthUsr(username, password));

            @Override
            MultiPartReader<AuthUsr> reader() {
                return reader;
            }
        };
    }
}
