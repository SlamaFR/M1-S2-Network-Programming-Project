package fr.upem.chatfusion.common.reader;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class MultiPartReader<T> implements Reader<T> {

    private enum State {
        WAITING, DONE, ERROR
    }

    private final List<PartReader<?>> partReaders;
    private final Supplier<T> factory;

    private State state = State.WAITING;

    public MultiPartReader(List<PartReader<?>> partReaders, Supplier<T> factory) {
        this.partReaders = Objects.requireNonNull(partReaders);
        this.factory = Objects.requireNonNull(factory);
    }

    @Override
    public ProcessStatus process(ByteBuffer buffer) {
        for (PartReader<?> partReader : partReaders) {
            var status = partReader.process(buffer);
            if (status != ProcessStatus.DONE) {
                if (status == ProcessStatus.ERROR) {
                    state = State.ERROR;
                }
                return status;
            }
        }
        state = State.DONE;
        return ProcessStatus.DONE;
    }

    @Override
    public T get() {
        if (state != State.DONE) {
            throw new IllegalStateException("MultiPartReader is not done");
        }
        return factory.get();
    }

    @Override
    public void reset() {
        for (PartReader<?> partReader : partReaders) {
            partReader.reset();
        }
    }

    public static <T> PartReader<T> part(Reader<T> reader, Consumer<T> consumer) {
        Objects.requireNonNull(reader);
        Objects.requireNonNull(consumer);
        return new PartReader<>(reader, consumer);
    }

    public static PartReader<Byte> getByte(Consumer<Byte> consumer) {
        Objects.requireNonNull(consumer);
        return part(new ByteReader(), consumer);
    }

    public static PartReader<Integer> getInt(Consumer<Integer> consumer) {
        Objects.requireNonNull(consumer);
        return part(new IntReader(), consumer);
    }

    public static PartReader<String> getString(Consumer<String> consumer) {
        Objects.requireNonNull(consumer);
        return part(new StringReader(), consumer);
    }

    public static PartReader<byte[]> getBytes(Consumer<byte[]> consumer) {
        Objects.requireNonNull(consumer);
        return part(new BytesArrayReader(), consumer);
    }

    public static <E> PartReader<List<E>> getList(Reader<E> reader, Consumer<List<E>> consumer) {
        Objects.requireNonNull(reader);
        Objects.requireNonNull(consumer);
        return part(new ListReader<>(reader), consumer);
    }

    public static PartReader<List<Integer>> getIntegerList(Consumer<List<Integer>> consumer) {
        Objects.requireNonNull(consumer);
        return getList(new IntReader(), consumer);
    }

    public static class PartReader<T> implements Reader<T> {

        private enum State {
            WAITING, DONE, ERROR
        }

        private final Reader<T> reader;
        private final Consumer<T> action;

        private State state = State.WAITING;

        public PartReader(Reader<T> reader, Consumer<T> action) {
            this.reader = Objects.requireNonNull(reader);
            this.action = Objects.requireNonNull(action);
        }

        @Override
        public ProcessStatus process(ByteBuffer buffer) {
            Objects.requireNonNull(buffer);
            if (state == State.DONE) {
                return ProcessStatus.DONE;
            }
            if (state == State.ERROR) {
                return ProcessStatus.ERROR;
            }
            var status = reader.process(buffer);
            if (status != ProcessStatus.DONE) {
                return status;
            }
            state = State.DONE;
            action.accept(reader.get());
            reader.reset();
            return ProcessStatus.DONE;
        }

        @Override
        public T get() {
            if (state != State.DONE) {
                throw new IllegalStateException("Reader is not done");
            }
            return reader.get();
        }

        @Override
        public void reset() {
            reader.reset();
        }
    }
}
