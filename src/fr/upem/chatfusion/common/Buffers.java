package fr.upem.chatfusion.common;

import java.nio.ByteBuffer;

public final class Buffers {

    private Buffers() {
        throw new AssertionError("No Buffers.class instance for you");
    }

    public static boolean tryPut(ByteBuffer dst, ByteBuffer src) {
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

}
