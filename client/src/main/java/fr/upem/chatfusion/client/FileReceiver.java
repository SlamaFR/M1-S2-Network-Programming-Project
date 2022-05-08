package fr.upem.chatfusion.client;

import fr.upem.chatfusion.common.packet.FileChunk;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

public final class FileReceiver implements AutoCloseable {

    private final Path path;

    private FileOutputStream outputStream;
    private int received;

    public FileReceiver(Path basePath, String filename) {
        this.path = Path.of(basePath + "/" + filename);
        this.received = 0;
    }

    public void init() throws IOException {
        if (outputStream != null) {
            throw new IllegalStateException("FileReceiver has already been initialized");
        }
        var file = path.toFile();
        if (!file.createNewFile()) {
            System.out.printf("File %s already exists\n", file.getName());
            throw new IOException("file already exists");
        }
        try {
            outputStream = new FileOutputStream(file, true);
        } catch (FileNotFoundException e) {
            throw new AssertionError(e);
        }
    }

    public void writeChunk(FileChunk packet) throws IOException {
        Objects.requireNonNull(packet);
        if (outputStream == null) {
            throw new IllegalStateException("FileReceiver has not been initialized yet");
        }
        outputStream.write(packet.chunk());
        if (++received == packet.chunkNumber()) {
            System.out.printf("[%d] %s: File %s received\n", packet.srcServerId(),
                    packet.srcNickname(), path.getFileName());
            close();
        }
    }

    @Override
    public void close() throws IOException {
        outputStream.close();
    }
}
