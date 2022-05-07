package fr.upem.chatfusion.client;


import fr.upem.chatfusion.common.packet.FileChunk;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.channels.Selector;
import java.nio.file.Path;

public class FileSender {

    private static final int CHUNK_SIZE = 5000;

    private final ServerContext ctx;
    private final Selector selector;
    private final int srcServerId;
    private final int dstServerId;
    private final String srcNickname;
    private final String dstNickname;
    private final String filename;
    private final FileInputStream inputStream;
    private final int chunkNumber;
    private final int transferID;

    public FileSender(ServerContext ctx, Selector selector, int srcServerId, int dstServerId, String srcNickname, int transferID, String dstNickname, Path path) throws IOException {
        this.ctx = ctx;
        this.selector = selector;
        this.srcServerId = srcServerId;
        this.dstServerId = dstServerId;
        this.dstNickname = dstNickname;
        this.srcNickname = srcNickname;
        this.transferID = transferID;
        var file = path.toFile();

        if (!file.exists()) {
            throw new IllegalArgumentException("File does not exist");
        }

        this.filename = file.getName();
        this.chunkNumber = (int) Math.ceil(file.length() / (double) CHUNK_SIZE);
        this.inputStream = new FileInputStream(file);
    }

    public void send() {
        try {
            while (inputStream.available() > 0) {
                var bytes = inputStream.readNBytes(Math.min(inputStream.available(), CHUNK_SIZE));
                var packet = new FileChunk(
                    srcServerId,
                    srcNickname,
                    dstServerId,
                    dstNickname,
                    transferID,
                    filename,
                    chunkNumber,
                    bytes.length,
                    bytes
                );
                ctx.enqueueFileChunk(packet);
            }
            selector.wakeup();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

}
