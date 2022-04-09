package fr.upem.chatfusion.common.packet;

import fr.upem.chatfusion.common.Buffers;
import fr.upem.chatfusion.common.reader.MultiPartReader;
import fr.upem.chatfusion.common.reader.Reader;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Objects;

import static java.nio.charset.StandardCharsets.UTF_8;

public record FileChunk(int serverId, String username, String filename, int chunkNumber, int chunkSize, byte[] chunk) implements Packet {

    public FileChunk {
        Objects.requireNonNull(username);
        Objects.requireNonNull(filename);
        Objects.requireNonNull(chunk);
    }

    @Override
    public ByteBuffer toByteBuffer() {
        var usernameBytes = UTF_8.encode(username);
        var filenameBytes = UTF_8.encode(filename);
        var buffer = ByteBuffer.allocate(Byte.BYTES + 5 * Integer.BYTES + usernameBytes.remaining() + filenameBytes.remaining() + chunk.length);

        buffer.put(OpCode.FILE_CHUNK.getCode());
        buffer.putInt(serverId);
        Buffers.putEncodedString(buffer, usernameBytes);
        Buffers.putEncodedString(buffer, filenameBytes);
        buffer.putInt(chunkNumber);
        buffer.putInt(chunkSize);
        buffer.put(chunk);
        return buffer;
    }

    @Override
    public void accept(PacketVisitor visitor) {
        Objects.requireNonNull(visitor);
        visitor.visit(this);
    }

    public static Reader<FileChunk> getReader() {
        return new AbstractPacketReader<>() {

            private int serverId;
            private String username;
            private String filename;
            private int chunkNumber;
            private byte[] chunk;

            private final MultiPartReader<FileChunk> reader = new MultiPartReader<>(List.of(
                    MultiPartReader.getInt(i -> serverId = i),
                    MultiPartReader.getString(s -> username = s),
                    MultiPartReader.getString(s -> filename = s),
                    MultiPartReader.getInt(i -> chunkNumber = i),
                    MultiPartReader.getBytes(b -> chunk = b)
            ), () -> new FileChunk(serverId, username, filename, chunkNumber, chunk.length, chunk));

            @Override
            MultiPartReader<FileChunk> reader() {
                return reader;
            }
        };
    }
}
