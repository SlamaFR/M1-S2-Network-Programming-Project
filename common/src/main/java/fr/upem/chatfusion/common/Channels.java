package fr.upem.chatfusion.common;

import java.io.Closeable;
import java.util.Objects;

public final class Channels {

    private Channels() {
        throw new AssertionError("No instances for you!");
    }

    public static void silentlyClose(Closeable closeable) {
        try {
            Objects.requireNonNull(closeable).close();
        } catch (Exception e) {
            // Ignore
        }
    }

}
