package fr.upem.chatfusion.common.message;

import fr.upem.chatfusion.common.Buffers;
import fr.upem.chatfusion.common.packet.Packet;

import java.nio.ByteBuffer;

import static java.nio.charset.StandardCharsets.UTF_8;

public record PrivateMessage(String nickname, String message) {

    /**
     * @return message encoded in UTF-8, in write mode
     */
    public ByteBuffer toByteBuffer() {
        var recipient = UTF_8.encode(nickname);
        var content = UTF_8.encode(message);
        var buffer = ByteBuffer.allocate(Byte.BYTES + 2 * Integer.BYTES + recipient.remaining() + content.remaining());

        buffer.put(Packet.OpCode.OUTGOING_PRIVATE_MESSAGE.getCode());
        Buffers.putEncodedString(buffer, recipient);
        Buffers.putEncodedString(buffer, content);

        return buffer;
    }

}
