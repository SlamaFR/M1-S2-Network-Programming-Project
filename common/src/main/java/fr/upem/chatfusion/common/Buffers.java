package fr.upem.chatfusion.common;

import java.nio.ByteBuffer;
import java.util.Objects;

public final class Buffers {

    private Buffers() {
        throw new AssertionError("No instances for you!");
    }

    /**
     * Tries to put the given {@code src} into the {@code dst} buffer.
     * <p>
     * <b>Notice:</b> The {@code dst} buffer must be in write mode. If the
     * {@code src} buffer is not big enough, the {@code dst} gets as much
     * data as possible.
     *
     * @param dst the destination buffer
     * @param src the source buffer
     * @return {@code true} if the {@code src} buffer was fully put into the
     *         {@code dst} buffer, {@code false} otherwise
     */
    public static boolean tryPut(ByteBuffer dst, ByteBuffer src) {
        Objects.requireNonNull(dst);
        Objects.requireNonNull(src);
        if (src.remaining() <= dst.remaining()) {
            dst.put(src);
            return true;
        } else {
            var oldLimit = src.limit();
            src.limit(dst.remaining());
            dst.put(src);
            src.limit(oldLimit);
            return false;
        }
    }

    /**
     * Puts the given {@code string} into the {@code dst} buffer.
     * <p>
     * <b>Notice:</b> The {@code dst} buffer must be big enough to contain
     * the {@code string} and should be in write mode.
     *
     * @param dst the destination buffer
     * @param string the string to put
     * @return the {@code dst} buffer
     */
    public static ByteBuffer putEncodedString(ByteBuffer dst, ByteBuffer string) {
        Objects.requireNonNull(dst);
        Objects.requireNonNull(string);

        dst.putInt(string.remaining());
        dst.put(string);
        return dst;
    }

}
