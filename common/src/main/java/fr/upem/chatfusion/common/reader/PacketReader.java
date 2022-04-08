package fr.upem.chatfusion.common.reader;

import fr.upem.chatfusion.common.packet.AuthGst;
import fr.upem.chatfusion.common.packet.AuthRsp;
import fr.upem.chatfusion.common.packet.AuthUsr;
import fr.upem.chatfusion.common.packet.Packet;

import java.nio.ByteBuffer;

public class PacketReader implements Reader<Packet> {

    private final ByteReader byteReader = new ByteReader();
    private final Reader<AuthGst> authGstReader = AuthGst.getReader();
    private final Reader<AuthUsr> authUsrReader = AuthUsr.getReader();
    private final Reader<AuthRsp> authRspReader = AuthRsp.getReader();

    private enum State {
        WAITING_OPCODE, WAITING_DATA, DONE, ERROR
    }

    private Reader<? extends Packet> reader;
    private State state = State.WAITING_OPCODE;

    private void updateReader() {
        this.reader = switch (Packet.OpCode.fromCode(byteReader.get())) {
            case AUTHENTICATION_GUEST -> authGstReader;
            case AUTHENTICATION_USER -> authUsrReader;
            case AUTHENTICATION_RESPONSE -> authRspReader;
            default -> null;
        };
    }

    @Override
    public ProcessStatus process(ByteBuffer buffer) {
        if (state == State.DONE || state == State.ERROR) {
            throw new IllegalStateException();
        }
        if (state == State.WAITING_OPCODE) {
            var status = byteReader.process(buffer);
            if (status != ProcessStatus.DONE) {
                return status;
            }
            updateReader();
            state = State.WAITING_DATA;

        }
        if (state == State.WAITING_DATA) {
            var status = reader.process(buffer);
            if (status != ProcessStatus.DONE) {
                return status;
            }
            state = State.DONE;
        }
        return ProcessStatus.DONE;
    }

    @Override
    public Packet get() {
        if (state != State.DONE) {
            throw new IllegalStateException();
        }
        return reader.get();
    }

    @Override
    public void reset() {
        byteReader.reset();
        reader.reset();
        reader = null;
        state = State.WAITING_OPCODE;
    }
}
