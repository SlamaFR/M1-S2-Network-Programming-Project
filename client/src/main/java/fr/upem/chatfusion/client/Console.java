package fr.upem.chatfusion.client;

import java.util.Scanner;
import java.util.logging.Logger;

public class Console implements Runnable {

    private static final Logger LOGGER = Logger.getLogger(Console.class.getName());

    private final Client client;

    public Console(Client client) {
        this.client = client;
    }

    @Override
    public void run() {
        try (var scanner = new Scanner(System.in)) {
            while (scanner.hasNextLine() && !Thread.interrupted()) {
                var command = scanner.nextLine();
                client.sendCommand(command);
            }
        } catch (InterruptedException e) {
            LOGGER.info("Console interrupted");
        }
    }
}
