package fr.upem.chatfusion.common.packet;

import fr.upem.chatfusion.common.Buffers;
import fr.upem.chatfusion.common.frame.PrivateMessageFrame;

import java.nio.ByteBuffer;

import static java.nio.charset.StandardCharsets.UTF_8;

public record IncomingPrivateMessage(int serverId, String nickname, String message) implements Packet {

    @Override
    public ByteBuffer toByteBuffer() {
        var messageBytes = UTF_8.encode(message);
        var recipientBytes = UTF_8.encode(nickname);
        var buffer = ByteBuffer.allocate(Byte.BYTES + 3 * Integer.BYTES + recipientBytes.remaining() + messageBytes.remaining());

        buffer.put(OpCode.INCOMING_PRIVATE_MESSAGE.getCode());
        buffer.putInt(serverId);
        Buffers.putEncodedString(buffer, recipientBytes);
        Buffers.putEncodedString(buffer, messageBytes);
        return buffer;
    }
}
