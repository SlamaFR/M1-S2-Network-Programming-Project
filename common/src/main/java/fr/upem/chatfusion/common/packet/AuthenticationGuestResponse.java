package fr.upem.chatfusion.common.packet;

import java.nio.ByteBuffer;

public record AuthenticationGuestResponse(AuthGuestResp code) implements Packet{

    public enum AuthGuestResp{
        AUTHENTICATION_GUEST_SUCCESS(0x00),
        AUTHENTICATION_GUEST_FAILED_NICKNAME_REGISTERED(0x01),
        AUTHENTICATION_GUEST_FAILED_COMBINATION(0x02),
        AUTHENTICATION_GUEST_FAILED_NICKNAME_GUEST(0x03);

        private final byte code;

        AuthGuestResp(int code) {
            this.code = (byte) code;
        }

        public byte getCode() {
            return code;
        }

        public static AuthGuestResp fromCode(byte code) {
            for (AuthGuestResp authGuestResp : values()) {
                if (authGuestResp.code == code) {
                    return authGuestResp;
                }
            }
            throw new IllegalArgumentException("Unknown AuthGuestRespCode: " + code);
        }
    }

    @Override
    public ByteBuffer toByteBuffer() {
        var buffer = ByteBuffer.allocate(Byte.BYTES * 2);
        buffer.put(OpCode.AUTHENTICATION_RESPONSE.getCode());
        buffer.put(code.getCode());
        return buffer;
    }

    @Override
    public String toString() {
        return "AuthenticationGuestResponse{" +
            "AUTHRSP='" + OpCode.AUTHENTICATION_RESPONSE.getCode() + '\'' +
            "OPCODE='" + code.getCode() + '\''+
            '}';
    }
}
