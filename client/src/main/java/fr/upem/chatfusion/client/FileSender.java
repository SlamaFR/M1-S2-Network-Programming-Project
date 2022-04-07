package fr.upem.chatfusion.client;

import fr.upem.chatfusion.common.packet.FileChunkFrame;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;

public class FileSender {

    private static final int CHUNK_SIZE = 5000;

    private final ServerContext ctx;
    private final int dstServerId;
    private final String receiver;
    private final String filename;
    private final FileInputStream inputStream;
    private final int chunkNumber;

    public FileSender(ServerContext ctx, int dstServerId, String recipient, Path path) throws IOException {
        this.ctx = ctx;
        this.dstServerId = dstServerId;
        this.receiver = recipient;
        var file = path.toFile();

        System.out.println(path);

        if (!file.exists()) {
            throw new IllegalArgumentException("File does not exist");
        }

        this.filename = file.getName();
        this.chunkNumber = (int) Math.ceil(file.length() / (double) CHUNK_SIZE);
        System.out.println(">> " + chunkNumber);
        this.inputStream = new FileInputStream(file);
    }

    public void sendAsync() {
        var thread = new Thread(() -> {
            try {
                while (inputStream.available() > 0) {
                    System.out.println("Sending chunk");
                    var bytes = inputStream.readNBytes(Math.min(inputStream.available(), CHUNK_SIZE));
                    var packet = new FileChunkFrame(
                            dstServerId,
                            receiver,
                            filename,
                            chunkNumber,
                            bytes.length,
                            ByteBuffer.wrap(bytes)
                    );
                    ctx.enqueue(packet);
                }
                System.out.println("ALL SENT");
                ctx.wakeup();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

}
