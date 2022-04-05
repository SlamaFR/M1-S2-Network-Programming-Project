package fr.upem.chatfusion.common.packet;

import fr.upem.chatfusion.common.Buffers;

import java.nio.ByteBuffer;

import static java.nio.charset.StandardCharsets.UTF_8;

public record AuthenticationGuest(String nickname) implements Packet {

    @Override
    public ByteBuffer toByteBuffer() {
        var usernameBytes = UTF_8.encode(nickname);
        var buffer = ByteBuffer.allocate(Byte.BYTES + Integer.BYTES + usernameBytes.remaining());

        buffer.put(Packet.OpCode.AUTHENTICATION_GUEST.getCode());
        Buffers.putEncodedString(buffer, usernameBytes);
        return buffer;
    }

    @Override
    public String toString() {
        return "AuthenticationGuest{" +
                "opCode='Ox" + Packet.OpCode.AUTHENTICATION_GUEST.getCode() + "' " +
                "nicknameSize=" + nickname.length() + " " +
                "nickname='" + nickname + '\'' +
                '}';
    }
}
