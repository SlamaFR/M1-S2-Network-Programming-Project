package fr.upem.chatfusion.server;

import java.util.logging.Logger;

public class Console implements Runnable {

    private static final Logger LOGGER = Logger.getLogger(Console.class.getName());

    private final Server server;

    public Console(Server server) {
        this.server = server;
    }

    @Override
    public void run() {
        try (var scanner = new java.util.Scanner(System.in)) {
            while (scanner.hasNextLine() && !Thread.interrupted()) {
                var command = scanner.nextLine();
                server.sendCommand(command);
            }
        } catch (InterruptedException e) {
            LOGGER.info("Console interrupted");
        }
    }
}
