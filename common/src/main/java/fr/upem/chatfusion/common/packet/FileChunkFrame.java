package fr.upem.chatfusion.common.packet;

import fr.upem.chatfusion.common.Buffers;

import java.nio.ByteBuffer;

import static java.nio.charset.StandardCharsets.UTF_8;

public record FileChunkFrame(
        int serverId,
        String nickname,
        String filename,
        int chunkNumber,
        int chunkSize,
        ByteBuffer chunkData
) implements Packet {

    @Override
    public ByteBuffer toByteBuffer() {
        var receiverBytes = UTF_8.encode(nickname);
        var filenameBytes = UTF_8.encode(filename);
        var buffer = ByteBuffer.allocate(1 + 7 * Integer.BYTES + receiverBytes.remaining() + filenameBytes.remaining() + chunkData.remaining());

        buffer.put(OpCode.FILE_CHUNK.getCode());
        buffer.putInt(serverId);
        Buffers.putEncodedString(buffer, receiverBytes);
        Buffers.putEncodedString(buffer, filenameBytes);
        buffer.putInt(chunkNumber);
        buffer.putInt(chunkSize);
        buffer.put(chunkData);
        return buffer;
    }
}
