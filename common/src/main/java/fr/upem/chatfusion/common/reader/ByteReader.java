package fr.upem.chatfusion.common.reader;

import fr.upem.chatfusion.common.Buffers;

import java.nio.ByteBuffer;

public class ByteReader implements Reader<Byte>{

    private enum State {
        DONE, WAITING, ERROR
    }

    private State state = State.WAITING;
    private final ByteBuffer internalBuffer = ByteBuffer.allocate(Byte.BYTES); // write-mode
    private byte value;

    @Override
    public ProcessStatus process(ByteBuffer buffer) {
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
        value = internalBuffer.get();
        return ProcessStatus.DONE;
    }

    @Override
    public Byte get() {
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
