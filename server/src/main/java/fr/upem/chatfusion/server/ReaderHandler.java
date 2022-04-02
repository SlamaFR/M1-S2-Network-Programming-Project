package fr.upem.chatfusion.server;

import fr.upem.chatfusion.common.packet.Packet;
import fr.upem.chatfusion.common.reader.Reader;

import java.nio.ByteBuffer;
import java.util.logging.Logger;

public final class ReaderHandler {

    private static final Logger LOGGER = Logger.getLogger(ReaderHandler.class.getName());

    private ReaderHandler() {
        throw new AssertionError("No fr.upem.chatfusion.server.ReaderHandler instances for you!");
    }

    /**
     * Tries to read a packet from the given buffer.
     *
     * @param reader The reader.
     * @param buffer The buffer to read from.
     * @return {@code true} if the packet was fully read,
     * {@code false} otherwise.
     */
    public static boolean handlePacketReader(Reader<? extends Packet> reader, ByteBuffer buffer) {
        switch (reader.process(buffer)) {
            case DONE -> {
                return true;
            }
            case ERROR -> {
                LOGGER.severe("Malformed message packet");
                return false;
            }
            default -> {
                return false;
            }
        }
    }

}
