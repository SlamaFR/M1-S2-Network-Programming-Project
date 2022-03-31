package fr.upem.chatfusion.common.message;

import java.nio.ByteBuffer;

import static java.nio.charset.StandardCharsets.UTF_8;

public record PublicMessage(String message) {

    /**
     * @return message encoded in UTF-8, in write mode
     */
    public ByteBuffer toByteBuffer() {
        var content = UTF_8.encode(message);
        var buffer = ByteBuffer.allocate(content.remaining() + Integer.BYTES);

        buffer.putInt(content.remaining());
        buffer.put(content);

        return buffer;
    }

}
