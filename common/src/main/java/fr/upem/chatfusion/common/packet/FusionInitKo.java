package fr.upem.chatfusion.common.packet;

import java.nio.ByteBuffer;

public record FusionInitKo() implements Packet {

    @Override
    public ByteBuffer toByteBuffer() {
        var buffer = ByteBuffer.allocate(Byte.BYTES);

        buffer.put(OpCode.FUSION_INIT_KO.getCode());
        return buffer;
    }

    @Override
    public void accept(PacketVisitor visitor) {
        visitor.visit(this);
    }
}
