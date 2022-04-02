package fr.upem.chatfusion.client;

import fr.upem.chatfusion.common.packet.AuthenticationGuest;
import fr.upem.chatfusion.common.packet.Packet;
import fr.upem.chatfusion.common.reader.AuthGuestReader;
import fr.upem.chatfusion.common.reader.Reader;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.ArrayDeque;
import java.util.Scanner;
import java.util.logging.Logger;

public class ConsoleAuth {

    static private class Context {
        private final SelectionKey key;
        private final SocketChannel sc;
        private final ByteBuffer bufferIn = ByteBuffer.allocate(2);
        private final ByteBuffer bufferOut = ByteBuffer.allocate(BUFFER_SIZE);
        private final ArrayDeque<Packet> queue = new ArrayDeque<>();
        private boolean closed = false;
        private final AuthGuestReader reader = new AuthGuestReader();
        private AuthenticationGuest guest;
        private Packet reponsePacket;



        private Context(SelectionKey key) {
            this.key = key;
            this.sc = (SocketChannel) key.channel();
        }

        /**
         * Process the content of bufferIn
         * Read the server Authentication Response packet
         * The convention is that bufferIn is in write-mode before the call to process
         * and after the call
         *
         */
        private void processIn() {
            var status  = reader.process(bufferIn);
            if (status == Reader.ProcessStatus.DONE){
                reponsePacket = reader.get();
                System.out.println(reponsePacket);
                bufferIn.clear();
                reader.reset();
            } else {
                bufferIn.compact();
            }

        }

        /**
         * Add a message to the message queue, tries to fill bufferOut and updateInterestOps
         *
         * @param msg
         */
        private void queueMessage(AuthenticationGuest msg) {
            queue.add(msg);
            processOut();
        }

        /**
         * Try to fill bufferOut from the Packet queue
         *
         */
        private void processOut() {
            if (bufferOut.remaining() < Integer.BYTES) {
                return;
            }
            guest = (AuthenticationGuest) queue.poll();
            if (guest == null) {
                return ;
            }
            bufferOut.put(guest.toByteBuffer().flip());
        }

        /**
         * Update the interestOps of the key looking only at values of the boolean
         * closed and of both ByteBuffers.
         *
         * The convention is that both buffers are in write-mode before the call to
         * updateInterestOps and after the call. Also it is assumed that process has
         * been be called just before updateInterestOps.
         */

        private void updateInterestOps() {
            if (bufferIn.position() == 0 && closed) {
                silentlyClose();
                return;
            }
            if (bufferOut.position() != 0) {
                key.interestOps(SelectionKey.OP_WRITE);
                return;
            }
            key.interestOps(SelectionKey.OP_READ);
        }

        private void silentlyClose() {
            try {
                sc.close();
            } catch (IOException e) {
                // ignore exception
            }
        }

        /**
         * Performs the read action on sc
         *
         * The convention is that both buffers are in write-mode before the call to
         * doRead and after the call
         *
         * @throws IOException
         */
        private void doRead() throws IOException {
            var numberOfBytesRead = sc.read(bufferIn);
            if ( numberOfBytesRead == -1) {
                closed = true;
                key.cancel();
            }
            else if (numberOfBytesRead == 0 ){
                return ;
            }
            else {
                processIn();
            }
        }

        /**
         * Performs the write action on sc
         *
         * The convention is that both buffers are in write-mode before the call to
         * doWrite and after the call
         *
         * @throws IOException
         */

        private void doWrite() throws IOException {
            bufferOut.flip();
            sc.write(bufferOut);
            bufferOut.compact();
        }

        public void doConnect() throws IOException {
            if (!sc.finishConnect()) {
                return; // the selector gave a bad hint
            }
        }
    }

    static private int BUFFER_SIZE = 10_000;
    static private Logger logger = Logger.getLogger(ConsoleAuth.class.getName());

    private final SocketChannel sc;
    private final Selector selector;
    private final Thread console;
    private Context uniqueContext;
    private final Object lock = new Object();
    private final SelectionKey key;
    private boolean isConnected = false;


    public ConsoleAuth(Selector selector, SocketChannel sc, SelectionKey key) throws IOException {
        this.selector = selector;
        this.console = new Thread(this::consoleRun);
        this.sc = sc;
        this.key = key;
    }

    private void consoleRun() {
        try {
            System.out.println("Choose your way of connection :");
            System.out.println("0 -> Authentication as a guest.");
            System.out.println("1 -> Authentication as a user.");
            try (var scanner = new Scanner(System.in)) {
                while (scanner.hasNextLine() && !isConnected) {
                    var authOp = Integer.parseInt(scanner.nextLine());
                    var nickname = scanner.nextLine();
                    switch (authOp){
                        case 0 :
                            sendGuestCommand(nickname);
                            break;
                        case 1:
                            var pwd = scanner.nextLine();
                            sendUserCommand(nickname, pwd);
                            break;
                        default:
                            System.out.println("INCORRECT WAY OF CONNECTION TRY AGAIN.");
                            break;
                    }
                    isConnected = true; // TODO : change this automatic log in
                    System.out.println("CONNECTED ! ");
                }
            }
            logger.info("Console thread stopping");
        } catch (InterruptedException e) {
            logger.info("Console thread has been interrupted");
        }

    }

    /**
     * Send instructions to the selector via a BlockingQueue and wake it up
     *
     * @param nickname
     * @throws InterruptedException
     */

    private void sendGuestCommand(String nickname) throws InterruptedException {
        synchronized (lock) {
            uniqueContext.queueMessage(new AuthenticationGuest(nickname));
            selector.wakeup();
        }
    }

    /**
     * Send instructions to the selector via a BlockingQueue and wake it up
     *
     * @param nickname
     * @param pwd
     * @throws InterruptedException
     */

    private void sendUserCommand(String nickname, String pwd) throws InterruptedException {

        /*synchronized (lock) {
            //uniqueContext.queueMessage(new AuthenticationGuest(nickname));
            selector.wakeup();
        }*/
    }

    /**
     * Processes the command from the BlockingQueue
     */
    private void processCommands() {
        synchronized (lock) {
            uniqueContext.updateInterestOps();
        }
    }

    public void launch() throws IOException {
        uniqueContext = new Context(key);
        key.attach(uniqueContext);
        console.start();

        while (!Thread.interrupted() && !isConnected) {
            try {
                selector.select(this::treatKey);
                processCommands();
            } catch (UncheckedIOException tunneled) {
                throw tunneled.getCause();
            }
        }
    }

    private void treatKey(SelectionKey key) {
        try {
            if (key.isValid() && key.isConnectable()) {
                uniqueContext.doConnect();
            }
            if (key.isValid() && key.isWritable()) {
                uniqueContext.doWrite();
            }
            if (key.isValid() && key.isReadable()) {
                uniqueContext.doRead();
            }
        } catch (IOException ioe) {
            // lambda call in select requires to tunnel IOException
            throw new UncheckedIOException(ioe);
        }
    }



}
