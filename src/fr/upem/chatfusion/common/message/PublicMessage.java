package fr.upem.chatfusion.common.message;

import fr.upem.chatfusion.common.Buffers;
import fr.upem.chatfusion.common.packet.Packet;

import java.nio.ByteBuffer;

import static java.nio.charset.StandardCharsets.UTF_8;

public record PublicMessage(String message) {

    /**
     * @return message encoded in UTF-8, in write mode
     */
    public ByteBuffer toByteBuffer() {
        var content = UTF_8.encode(message);
        var buffer = ByteBuffer.allocate(Byte.BYTES + Integer.BYTES + content.remaining());

        buffer.put(Packet.OpCode.OUTGOING_PUBLIC_MESSAGE.getCode());
        Buffers.putEncodedString(buffer, content);

        return buffer;
    }

}
