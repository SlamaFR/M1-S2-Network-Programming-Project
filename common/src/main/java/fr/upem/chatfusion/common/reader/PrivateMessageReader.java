package fr.upem.chatfusion.common.reader;

import fr.upem.chatfusion.common.packet.PrivateMessage;

import java.nio.ByteBuffer;

public class PrivateMessageReader implements Reader<PrivateMessage> {

    private enum State {
        DONE, WAITING_SERVER_ID, WAITING_RECIPIENT, WAITING_MESSAGE, ERROR
    }

    private final IntReader intReader;
    private final StringReader stringReader;
    private PrivateMessage packet;
    private State state;

    private int serverId;
    private String nickname;
    private String message;

    public PrivateMessageReader() {
        this.intReader = new IntReader();
        this.stringReader = new StringReader();
        this.state = State.WAITING_SERVER_ID;
    }

    @Override
    public ProcessStatus process(ByteBuffer buffer) {
        if (state == State.DONE || state == State.ERROR) {
            throw new IllegalStateException();
        }
        if (state == State.WAITING_SERVER_ID) {
            var status = intReader.process(buffer);
            if (status != ProcessStatus.DONE) {
                return status;
            }
            serverId = intReader.get();
            state = State.WAITING_RECIPIENT;
        }
        System.out.println(serverId);
        if (state == State.WAITING_RECIPIENT) {
            var status = stringReader.process(buffer);
            if (status != ProcessStatus.DONE) {
                return status;
            }
            nickname = stringReader.get();
            stringReader.reset();
            state = State.WAITING_MESSAGE;
        }
        System.out.println(nickname);
        if (state == State.WAITING_MESSAGE) {
            var status = stringReader.process(buffer);
            if (status != ProcessStatus.DONE) {
                return status;
            }
            message = stringReader.get();
        }
        System.out.println(message);
        state = State.DONE;
        packet = new PrivateMessage(serverId, nickname, message);
        return ProcessStatus.DONE;
    }

    @Override
    public PrivateMessage get() {
        if (state != State.DONE) {
            throw new IllegalStateException();
        }
        return packet;
    }

    @Override
    public void reset() {
        state = State.WAITING_SERVER_ID;
        intReader.reset();
        stringReader.reset();
    }
}
