package fr.upem.chatfusion.common.packet;

import fr.upem.chatfusion.common.Buffers;

import java.nio.ByteBuffer;

import static java.nio.charset.StandardCharsets.UTF_8;

public record IncomingPublicMessage(String author, String message) implements Packet {

    @Override
    public ByteBuffer toByteBuffer() {
        var authorBytes = UTF_8.encode(author);
        var messageBytes = UTF_8.encode(message);
        var buffer = ByteBuffer.allocate(Byte.BYTES + 2 * Integer.BYTES + authorBytes.remaining() + messageBytes.remaining());

        buffer.put(OpCode.INCOMING_PUBLIC_MESSAGE.getCode());
        Buffers.putEncodedString(buffer, authorBytes);
        Buffers.putEncodedString(buffer, messageBytes);
        return buffer;
    }

    @Override
    public String toString() {
        return author() + ": " + message();
    }
}
