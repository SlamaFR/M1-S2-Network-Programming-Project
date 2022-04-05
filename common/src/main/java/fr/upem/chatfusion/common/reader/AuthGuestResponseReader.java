package fr.upem.chatfusion.common.reader;
import fr.upem.chatfusion.common.packet.AuthenticationGuestResponse;

import java.nio.ByteBuffer;

public class AuthGuestResponseReader implements Reader<AuthenticationGuestResponse> {

    private enum State {
        DONE, WAITING, ERROR
    }


    private final ByteReader reader;
    private AuthenticationGuestResponse packet;
    private State state;

    public AuthGuestResponseReader() {
        this.reader = new ByteReader();
        this.state = State.WAITING;
    }

    @Override
    public ProcessStatus process(ByteBuffer buffer) {
        if (state == State.DONE || state == State.ERROR) {
            throw new IllegalStateException();
        }
        var status = reader.process(buffer);
        if( status != ProcessStatus.DONE) {
            return status;
        }
        state = State.DONE;
        packet = new AuthenticationGuestResponse(AuthenticationGuestResponse.AuthGuestResp.fromCode(reader.get()));
        return ProcessStatus.DONE;
    }

    @Override
    public AuthenticationGuestResponse get() {
        if (state != State.DONE) {
            throw new IllegalStateException();
        }
        return packet;
    }

    @Override
    public void reset() {
        state = State.WAITING;
        reader.reset();
    }
}
