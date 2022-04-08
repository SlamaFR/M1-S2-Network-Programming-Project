package fr.upem.chatfusion.common.packet;

import fr.upem.chatfusion.common.reader.MultiPartReader;
import fr.upem.chatfusion.common.reader.Reader;

import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.List;

public record FusionInit(InetSocketAddress address, List<Integer> neighbors) implements Packet {

    @Override
    public ByteBuffer toByteBuffer() {
        var addressBytes = address.getAddress().getAddress();
        var addressPort = address.getPort();
        var buffer = ByteBuffer.allocate(Byte.BYTES + Integer.BYTES + addressBytes.length * Byte.BYTES + Integer.BYTES + Integer.BYTES + neighbors.size());

        buffer.put(OpCode.FUSION_INIT.getCode());
        buffer.putInt(addressBytes.length);
        buffer.put(addressBytes);
        buffer.putInt(addressPort);
        for (var neighbor : neighbors) {
            buffer.putInt(neighbor);
        }
        return buffer;
    }

    @Override
    public void accept(PacketVisitor visitor) {
        visitor.visit(this);
    }

    public static Reader<FusionInit> getReader() {
        return new AbstractPacketReader<>() {

            private byte[] bytes;
            private int port;
            private List<Integer> neighbors;

            private final MultiPartReader<FusionInit> reader = new MultiPartReader<>(List.of(
                    MultiPartReader.getBytes(b -> bytes = b),
                    MultiPartReader.getInt(i -> port = i),
                    MultiPartReader.getIntegerList(i -> neighbors = i)
            ), () -> {
                try {
                    return new FusionInit(new InetSocketAddress(InetAddress.getByAddress(bytes), port), neighbors);
                } catch (UnknownHostException e) {
                    throw new UncheckedIOException(e);
                }
            });

            @Override
            MultiPartReader<FusionInit> reader() {
                return reader;
            }
        };
    }
}