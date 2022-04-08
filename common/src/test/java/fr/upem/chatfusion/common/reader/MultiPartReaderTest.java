package fr.upem.chatfusion.common.reader;

import fr.upem.chatfusion.common.packet.MsgPbl;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MultiPartReaderTest {

    @Test
    public void test1() {
        AtomicInteger serverId = new AtomicInteger();
        AtomicReference<String> nickname = new AtomicReference<>();
        AtomicReference<String> message = new AtomicReference<>();

        MultiPartReader<MsgPbl> frameReader = new MultiPartReader<>(List.of(
                MultiPartReader.getInt(serverId::set),
                MultiPartReader.getString(nickname::set),
                MultiPartReader.getString(message::set)
        ), () -> new MsgPbl(serverId.get(), nickname.get(), message.get()));

        var nick = "Test";
        var msg = "Hello";
        var buffer = ByteBuffer.allocate(12 + nick.length() + msg.length());
        buffer.putInt(1);
        buffer.putInt(nick.length());
        buffer.put(nick.getBytes());
        buffer.putInt(msg.length());
        buffer.put(msg.getBytes());

        var status = frameReader.process(buffer);
        assertEquals(Reader.ProcessStatus.DONE, status);

        var frame = frameReader.get();
        assertEquals(1, frame.serverId());
        assertEquals(nick, frame.nickname());
        assertEquals(msg, frame.message());
    }

}
