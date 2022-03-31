package fr.upem.chatfusion.common.message;

import java.nio.ByteBuffer;

import static java.nio.charset.StandardCharsets.UTF_8;

public record PrivateMessage(String nickname, String message) {

    /**
     * @return message encoded in UTF-8, in write mode
     */
    public ByteBuffer toByteBuffer() {
        var recipient = UTF_8.encode(nickname);
        var content = UTF_8.encode(message);
        var buffer = ByteBuffer.allocate(recipient.remaining() + content.remaining() + 2 * Integer.BYTES);

        buffer.putInt(recipient.remaining());
        buffer.put(recipient);
        buffer.putInt(content.remaining());
        buffer.put(content);

        return buffer;
    }

}
