package fr.upem.chatfusion.common;

import java.nio.channels.Channel;
import java.nio.channels.SelectionKey;

public final class Channels {

    private Channels() {
        throw new AssertionError("No instances for you!");
    }

    public static void silentlyClose(Channel channel) {
        try {
            channel.close();
        } catch (Exception e) {
            // Ignore
        }
    }

    public static void silentlyClose(SelectionKey key) {
        silentlyClose(key.channel());
    }

}
