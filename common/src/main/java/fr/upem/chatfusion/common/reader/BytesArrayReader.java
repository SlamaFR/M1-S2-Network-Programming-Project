package fr.upem.chatfusion.common.reader;

import fr.upem.chatfusion.common.Buffers;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Objects;

public class BytesArrayReader implements Reader<byte[]> {

    private enum State {
        WAITING_SIZE, WAITING_BYTES, DONE, ERROR
    }

    private final IntReader intReader = new IntReader();

    private State state = State.WAITING_SIZE;
    private ByteBuffer internalBuffer;

    @Override
    public ProcessStatus process(ByteBuffer buffer) {
        Objects.requireNonNull(buffer);
        if (state == State.DONE || state == State.ERROR) {
            throw new IllegalStateException();
        }
        if (state == State.WAITING_SIZE) {
            var status = intReader.process(buffer);
            if (status != ProcessStatus.DONE) {
                return status;
            }
            internalBuffer = ByteBuffer.allocate(intReader.get());
            state = State.WAITING_BYTES;
        }
        if (state == State.WAITING_BYTES) {
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
        internalBuffer.flip();
        state = State.DONE;
        return ProcessStatus.DONE;
    }

    @Override
    public byte[] get() {
        if (state != State.DONE) {
            throw new IllegalStateException();
        }
        var bytes = internalBuffer.array();
        return Arrays.copyOf(bytes, bytes.length);
    }

    @Override
    public void reset() {
        state = State.WAITING_SIZE;
        internalBuffer.clear();
    }
}
