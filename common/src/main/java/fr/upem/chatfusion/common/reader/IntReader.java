package fr.upem.chatfusion.common.reader;

import fr.upem.chatfusion.common.Buffers;

import java.nio.ByteBuffer;
import java.util.Objects;

public class IntReader implements Reader<Integer> {

    private enum State {
        DONE, WAITING, ERROR
    }

    private final ByteBuffer internalBuffer = ByteBuffer.allocate(Integer.BYTES);

    private State state = State.WAITING;
    private int value;

    @Override
    public ProcessStatus process(ByteBuffer buffer) {
        Objects.requireNonNull(buffer);
        if (state == State.DONE || state == State.ERROR) {
            throw new IllegalStateException();
        }
        buffer.flip();
        try {
            Buffers.tryPut(internalBuffer, buffer);
        } finally {
            buffer.compact();
        }
        if (internalBuffer.hasRemaining()) {
            return ProcessStatus.REFILL;
        }
        state = State.DONE;
        internalBuffer.flip();
        value = internalBuffer.getInt();
        return ProcessStatus.DONE;
    }

    @Override
    public Integer get() {
        if (state != State.DONE) {
            throw new IllegalStateException();
        }
        return value;
    }

    @Override
    public void reset() {
        state = State.WAITING;
        internalBuffer.clear();
    }
}