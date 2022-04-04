package fr.upem.chatfusion.common.packet;

import fr.upem.chatfusion.common.Buffers;

import java.nio.ByteBuffer;

import static java.nio.charset.StandardCharsets.UTF_8;

public record OutgoingPublicMessage(String message) implements Packet {

    @Override
    public ByteBuffer toByteBuffer() {
        var messageBytes = UTF_8.encode(message);
        var buffer = ByteBuffer.allocate(Byte.BYTES + Integer.BYTES + messageBytes.remaining());

        buffer.put(OpCode.OUTGOING_PUBLIC_MESSAGE.getCode());
        Buffers.putEncodedString(buffer, messageBytes);
        return buffer;
    }
}
