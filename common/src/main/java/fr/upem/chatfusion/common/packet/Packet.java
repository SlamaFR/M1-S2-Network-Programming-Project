package fr.upem.chatfusion.common.packet;

import java.nio.ByteBuffer;

public interface Packet {

    /**
     * Returns the packet encoded in a byte buffer.
     */
    ByteBuffer toByteBuffer();

    /**
     * Accepts a packet visitor.
     */
    void accept(PacketVisitor visitor);

    enum OpCode {
        AUTHENTICATION_GUEST(0x00),
        AUTHENTICATION_USER(0x01),

        AUTHENTICATION_RESPONSE(0x10),

        PUBLIC_MESSAGE(0x20),

        PRIVATE_MESSAGE(0x30),

        FILE_CHUNK(0x40),

        FUSION_INIT(0xA0),
        FUSION_INIT_OK(0xA1),
        FUSION_INIT_KO(0xA2),
        FUSION_INIT_FORWARD(0xA3),

        FUSION_REQUEST(0xB0),
        FUSION_REQUEST_RESPONSE(0xB1),

        FUSION_CHANGE_LEADER(0xC0),
        FUSION_ACKNOWLEDGE_LEADER(0xC1);

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
