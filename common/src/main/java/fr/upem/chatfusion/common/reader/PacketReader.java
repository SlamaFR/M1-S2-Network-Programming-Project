package fr.upem.chatfusion.common.reader;

import fr.upem.chatfusion.common.packet.AuthGst;
import fr.upem.chatfusion.common.packet.AuthRsp;
import fr.upem.chatfusion.common.packet.AuthUsr;
import fr.upem.chatfusion.common.packet.FileChunk;
import fr.upem.chatfusion.common.packet.FusionAckLeader;
import fr.upem.chatfusion.common.packet.FusionChangeLeader;
import fr.upem.chatfusion.common.packet.FusionInit;
import fr.upem.chatfusion.common.packet.FusionInitFwd;
import fr.upem.chatfusion.common.packet.FusionInitKo;
import fr.upem.chatfusion.common.packet.FusionInitOk;
import fr.upem.chatfusion.common.packet.FusionReq;
import fr.upem.chatfusion.common.packet.FusionRsp;
import fr.upem.chatfusion.common.packet.MsgPbl;
import fr.upem.chatfusion.common.packet.MsgPrv;
import fr.upem.chatfusion.common.packet.Packet;

import java.nio.ByteBuffer;
import java.util.Objects;

public class PacketReader implements Reader<Packet> {

    private final ByteReader byteReader = new ByteReader();

    private enum State {
        WAITING_OPCODE, WAITING_DATA, DONE, ERROR
    }

    private Reader<? extends Packet> reader;
    private State state = State.WAITING_OPCODE;

    private void updateReader() {
        var opCode = Packet.OpCode.fromCode(byteReader.get());
        this.reader = switch (opCode) {
            case AUTHENTICATION_GUEST -> AuthGst.getReader();
            case AUTHENTICATION_USER -> AuthUsr.getReader();
            case AUTHENTICATION_RESPONSE -> AuthRsp.getReader();
            case FILE_CHUNK -> FileChunk.getReader();
            case FUSION_ACKNOWLEDGE_LEADER -> FusionAckLeader.getReader();
            case FUSION_CHANGE_LEADER -> FusionChangeLeader.getReader();
            case FUSION_INIT -> FusionInit.getReader();
            case FUSION_INIT_FORWARD -> FusionInitFwd.getReader();
            case FUSION_INIT_OK -> FusionInitOk.getReader();
            case FUSION_INIT_KO -> FusionInitKo.getReader();
            case FUSION_REQUEST -> FusionReq.getReader();
            case FUSION_REQUEST_RESPONSE -> FusionRsp.getReader();
            case PUBLIC_MESSAGE -> MsgPbl.getReader();
            case PRIVATE_MESSAGE -> MsgPrv.getReader();
        };
    }

    @Override
    public ProcessStatus process(ByteBuffer buffer) {
        Objects.requireNonNull(buffer);
        if (state == State.DONE || state == State.ERROR) {
            throw new IllegalStateException();
        }
        if (state == State.WAITING_OPCODE) {
            var status = byteReader.process(buffer);
            if (status != ProcessStatus.DONE) {
                return status;
            }
            state = State.WAITING_DATA;
            updateReader();
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
