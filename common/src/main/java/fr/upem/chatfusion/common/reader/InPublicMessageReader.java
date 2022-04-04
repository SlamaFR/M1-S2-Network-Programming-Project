package fr.upem.chatfusion.common.reader;

import fr.upem.chatfusion.common.packet.IncomingPublicMessage;

import java.nio.ByteBuffer;

public class InPublicMessageReader implements Reader<IncomingPublicMessage> {

    private enum State {
        DONE, WAITING, ERROR
    }

    private final StringReader stringReader;
    private IncomingPublicMessage packet;
    private State state;

    private String nickname;
    private String message;

    public InPublicMessageReader() {
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
        nickname = stringReader.get();
        stringReader.reset();
        var messageState = stringReader.process(buffer);
        if (messageState != ProcessStatus.DONE) {
            return messageState;
        }
        message = stringReader.get();
        state = State.DONE;
        packet = new IncomingPublicMessage(nickname, message);
        return ProcessStatus.DONE;
    }

    @Override
    public IncomingPublicMessage get() {
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
