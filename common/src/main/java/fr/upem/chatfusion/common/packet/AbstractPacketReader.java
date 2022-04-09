package fr.upem.chatfusion.common.packet;

import fr.upem.chatfusion.common.reader.MultiPartReader;
import fr.upem.chatfusion.common.reader.Reader;

import java.nio.ByteBuffer;
import java.util.Objects;

abstract class AbstractPacketReader<T extends Packet> implements Reader<T> {

    abstract MultiPartReader<T> reader();

    @Override
    public ProcessStatus process(ByteBuffer buffer) {
        Objects.requireNonNull(buffer);
        return reader().process(buffer);
    }

    @Override
    public T get() {
        return reader().get();
    }

    @Override
    public void reset() {
        reader().reset();
    }
}
