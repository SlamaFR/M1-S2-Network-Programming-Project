package fr.upem.chatfusion.common.reader;

import fr.upem.chatfusion.common.packet.AuthenticationGuest;

import java.nio.ByteBuffer;

public class AuthGuestReader implements Reader<AuthenticationGuest> {

    private enum State {
        DONE, WAITING, ERROR
    }

    private final StringReader stringReader;
    private AuthenticationGuest packet;
    private State state;

    private String nickname;

    public AuthGuestReader() {
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
        state = State.DONE;
        packet = new AuthenticationGuest(nickname);
        return ProcessStatus.DONE;
    }

    @Override
    public AuthenticationGuest get() {
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
