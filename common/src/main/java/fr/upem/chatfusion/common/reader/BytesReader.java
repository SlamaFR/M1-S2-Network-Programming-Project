package fr.upem.chatfusion.common.reader;

import fr.upem.chatfusion.common.Buffers;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class BytesReader implements Reader<ByteBuffer> {

    private enum State {
        DONE, WAITING_LENGTH, WAITING_CONTENT, ERROR
    }

    private final IntReader intReader = new IntReader();
    private State state = State.WAITING_LENGTH;

    private int messageLength;
    private ByteBuffer internalBuffer;

    private ProcessStatus getMessageLength(ByteBuffer buffer) {
        var state = intReader.process(buffer);
        if (state != ProcessStatus.DONE) {
            return state;
        }
        messageLength = intReader.get();
        return ProcessStatus.DONE;
    }

    @Override
    public ProcessStatus process(ByteBuffer buffer) {
        if (state == State.DONE || state == State.ERROR) {
            throw new IllegalStateException();
        }
        if (state == State.WAITING_LENGTH) {
            var lengthState = getMessageLength(buffer);
            if (lengthState != ProcessStatus.DONE) {
                return lengthState;
            }
            internalBuffer = ByteBuffer.allocate(messageLength);
            state = State.WAITING_CONTENT;
        }
        if (state == State.WAITING_CONTENT) {
            buffer.flip();
            try {
                Buffers.tryPut(internalBuffer, buffer);
            } finally {
                buffer.compact();
            }
        }
        if (internalBuffer.hasRemaining()) {
            return ProcessStatus.REFILL;
        }
        state = State.DONE;
        internalBuffer.flip();
        return ProcessStatus.DONE;
    }

    @Override
    public ByteBuffer get() {
        if (state != State.DONE) {
            throw new IllegalStateException();
        }
        return internalBuffer;
    }

    @Override
    public void reset() {
        state = State.WAITING_LENGTH;
        internalBuffer.clear();
        intReader.reset();
    }
}