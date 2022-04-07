package fr.upem.chatfusion.common.reader;

import fr.upem.chatfusion.common.packet.FileChunkFrame;

import java.nio.ByteBuffer;

public class FileChunkReader implements Reader<FileChunkFrame> {

    private enum State {
        DONE,
        WAITING_SERVER_ID,
        WAITING_NICKNAME,
        WAITING_FILE_NAME,
        WAITING_FILE_CHUNK_NUMBER,
        WAITING_FILE_CHUNK_DATA,
        ERROR
    }

    private final IntReader intReader;
    private final StringReader stringReader;
    private final BytesReader bytesReader;
    private FileChunkFrame frame;
    private State state;

    private int serverId;
    private String nickname;
    private String fileName;
    private int chunkNumber;
    private ByteBuffer data;

    public FileChunkReader() {
        this.intReader = new IntReader();
        this.stringReader = new StringReader();
        this.bytesReader = new BytesReader();
        this.state = State.WAITING_SERVER_ID;
    }

    @Override
    public ProcessStatus process(ByteBuffer buffer) {
        if (state == State.DONE || state == State.ERROR) {
            throw new IllegalStateException();
        }
        if (state == State.WAITING_SERVER_ID) {
            var status = intReader.process(buffer);
            if (status != ProcessStatus.DONE) {
                return status;
            }
            serverId = intReader.get();
            intReader.reset();
            state = State.WAITING_NICKNAME;
        }
        if (state == State.WAITING_NICKNAME) {
            var status = stringReader.process(buffer);
            if (status != ProcessStatus.DONE) {
                return status;
            }
            nickname = stringReader.get();
            stringReader.reset();
            state = State.WAITING_FILE_NAME;
        }
        if (state == State.WAITING_FILE_NAME) {
            var status = stringReader.process(buffer);
            if (status != ProcessStatus.DONE) {
                return status;
            }
            fileName = stringReader.get();
            stringReader.reset();
            state = State.WAITING_FILE_CHUNK_NUMBER;
        }
        if (state == State.WAITING_FILE_CHUNK_NUMBER) {
            var status = intReader.process(buffer);
            if (status != ProcessStatus.DONE) {
                return status;
            }
            chunkNumber = intReader.get();
            intReader.reset();
            state = State.WAITING_FILE_CHUNK_DATA;
        }
        if (state == State.WAITING_FILE_CHUNK_DATA) {
            var status = bytesReader.process(buffer);
            if (status != ProcessStatus.DONE) {
                return status;
            }
            data = bytesReader.get();
        }
        state = State.DONE;
        frame = new FileChunkFrame(serverId, nickname, fileName, chunkNumber, data.capacity(), data);
        return ProcessStatus.DONE;
    }

    @Override
    public FileChunkFrame get() {
        if (state != State.DONE) {
            throw new IllegalStateException();
        }
        return frame;
    }

    @Override
    public void reset() {
        state = State.WAITING_SERVER_ID;
        intReader.reset();
        stringReader.reset();
        bytesReader.reset();
    }
}
