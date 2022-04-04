package fr.upem.chatfusion.common.reader;

import fr.upem.chatfusion.common.packet.AuthenticationGuest;
import fr.upem.chatfusion.common.packet.IncomingPublicMessage;
import fr.upem.chatfusion.common.packet.OutgoingPublicMessage;

import java.nio.ByteBuffer;

public class OutPublicMessageReader implements Reader<OutgoingPublicMessage> {

    private enum State {
        DONE, WAITING, ERROR
    }

    private final StringReader stringReader;
    private OutgoingPublicMessage packet;
    private State state;

    private String message;

    public OutPublicMessageReader() {
        this.stringReader = new StringReader();
        this.state = State.WAITING;
    }

    @Override
    public ProcessStatus process(ByteBuffer buffer) {
        if (state == State.DONE || state == State.ERROR) {
            throw new IllegalStateException();
        }
        var nicknameState = stringReader.process(buffer);
        if (nicknameState != ProcessStatus.DONE) {
            return nicknameState;
        }
        message = stringReader.get();
        state = State.DONE;
        packet = new OutgoingPublicMessage(message);
        return ProcessStatus.DONE;
    }

    @Override
    public OutgoingPublicMessage get() {
        if (state != State.DONE) {
            throw new IllegalStateException();
        }
        return packet;
    }

    @Override
    public void reset() {
        state = State.WAITING;
        stringReader.reset();
    }
}
