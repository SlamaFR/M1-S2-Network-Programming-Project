package fr.upem.chatfusion.common.reader;

import java.nio.ByteBuffer;

/**
 * This class is used to read data from a byte buffer.
 * @param <T> The type of data to read.
 */
public interface Reader<T> {

    enum ProcessStatus {
        /**
         * The object has been read.
         */
        DONE,
        /**
         * The object is not complete and the buffer is empty.
         */
        REFILL,
        /**
         * An error occurred.
         */
        ERROR
    }

    /**
     * Tries to read the next object from the buffer.
     *
     * @param buffer the buffer to read from
     * @return the status of the process
     */
    ProcessStatus process(ByteBuffer buffer);

    /**
     * Returns the object read.
     *
     * @return the object read
     * @throws IllegalStateException if the process is not done
     */
    T get();

    /**
     * Resets the reader.
     */
    void reset();

}
