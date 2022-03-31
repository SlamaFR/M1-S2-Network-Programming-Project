package fr.upem.chatfusion.common.packet;

import java.nio.ByteBuffer;

public sealed interface Packet permits AbstractPacket {

    /**
     * Writes down the packet into the given buffer.
     * <p>
     * <b>Note:</b> The buffer must be in write mode before and after this method call.
     */
    void write(ByteBuffer buffer);

    enum OpCode {
        AUTHENTICATION_GUEST(0x00),
        AUTHENTICATION_USER(0x01),
        AUTHENTICATION_RESPONSE(0x10),

        OUTGOING_PUBLIC_MESSAGE(0x20),
        INCOMING_PUBLIC_MESSAGE(0x30),

        OUTGOING_PRIVATE_MESSAGE(0x21),
        INCOMING_PRIVATE_MESSAGE(0x31),

        OUTGOING_FILE_CHUNK(0x40),
        INCOMING_FILE_CHUNK(0x41),

        PACKET_FORWARD(0x50),

        ERROR(0xF0);

        private final byte code;

        OpCode(int code) {
            this.code = (byte) code;
        }

        public byte getCode() {
            return code;
        }

        public static OpCode fromCode(byte code) {
            for (OpCode opCode : values()) {
                if (opCode.code == code) {
                    return opCode;
                }
            }
            throw new IllegalArgumentException("Unknown OpCode: " + code);
        }
    }

}
