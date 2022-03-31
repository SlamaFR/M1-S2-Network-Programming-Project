package fr.upem.chatfusion.common.packet;

import java.nio.ByteBuffer;

abstract non-sealed class AbstractPacket implements Packet {

    private final OpCode code;

    public AbstractPacket(OpCode code) {
        this.code = code;
    }

    @Override
    public void write(ByteBuffer buffer) {
        buffer.clear();
        buffer.put(code.getCode());
    }

    public OpCode getCode() {
        return code;
    }

}
