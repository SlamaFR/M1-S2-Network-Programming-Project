package fr.upem.chatfusion.common.context;

import java.io.Closeable;
import java.io.IOException;

public interface Context extends Closeable {

    /**
     * Performs a read on the channel.
     * <p>
     * <b>Notice:</b> All buffers should be in write mode.
     *
     * @throws IOException if an I/O error occurs
     */
    void doRead() throws IOException;

    /**
     * Performs a write on the channel.
     * <p>
     * <b>Notice:</b> All buffers should be in read mode.
     *
     * @throws IOException if an I/O error occurs
     */
    void doWrite() throws IOException;

    /**
     * Performs a connection on the channel.
     * <p>
     * <b>Notice:</b> All buffers should be in write mode.
     *
     * @throws IOException if an I/O error occurs
     */
    void doConnect() throws IOException;

    /**
     * Updates the interest ops of the channel based on the state
     * of the buffers.
     */
    void updateInterestOps();

    void processIn();

    void processOut();

}
