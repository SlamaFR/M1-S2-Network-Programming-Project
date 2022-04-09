package fr.upem.chatfusion.common.reader;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ListReader<E> implements Reader<List<E>> {

    private enum State {
        WAITING_SIZE, WAITING_ITEMS, DONE, ERROR
    }

    private final IntReader intReader = new IntReader();
    private final Reader<E> reader;

    private State state = State.WAITING_SIZE;
    private int size;
    private List<E> list = new ArrayList<>();

    public ListReader(Reader<E> reader) {
        this.reader = reader;
    }

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
            size = intReader.get();
            state = State.WAITING_ITEMS;
        }
        if (state == State.WAITING_ITEMS) {
            while (list.size() < size) {
                var status = reader.process(buffer);
                if (status != ProcessStatus.DONE) {
                    return status;
                }
                list.add(reader.get());
                reader.reset();
            }
        }
        state = State.DONE;
        return ProcessStatus.DONE;
    }

    @Override
    public List<E> get() {
        if (state != State.DONE) {
            throw new IllegalStateException();
        }
        return List.copyOf(list);
    }

    @Override
    public void reset() {
        state = State.WAITING_SIZE;
        list = new ArrayList<>();
    }
}
