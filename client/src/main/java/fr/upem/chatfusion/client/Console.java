package fr.upem.chatfusion.client;

import sun.misc.Unsafe;

import java.util.Objects;
import java.util.Scanner;
import java.util.logging.Logger;

public class Console implements Runnable {

    private static final Logger logger = Logger.getLogger(Console.class.getName());

    private final Client client;

    public Console(Client client) {
        this.client = Objects.requireNonNull(client);;
    }

    @Override
    public void run() {
        try (var scanner = new Scanner(System.in)) {
            while (scanner.hasNextLine()) {
                var line = scanner.nextLine();
                if (!line.isEmpty()) {
                    processLine(line);
                }
            }
        } catch (InterruptedException e) {
            throw new AssertionError(e);
        }
    }

    private void processLine(String line) throws InterruptedException {
        if (line.startsWith("@")) {
            try {
                var args = line.substring(1).split(" ", 2);
                var prefix = args[0].split(":");
                var nickname = prefix[0];
                var serverId = Integer.parseInt(prefix[1]);
                client.sendPrivateMessage(serverId, nickname, args[1]);
            } catch (NumberFormatException e) {
                System.out.println("Usage: @<nickname>:<serverId> <message>");
            }
        } else if (line.startsWith("/")) {
            try {
                var args = line.substring(1).split(" ", 2);
                var prefix = args[0].split(":");
                var nickname = prefix[0];
                var serverId = Integer.parseInt(prefix[1]);
                client.sendFile(serverId, nickname, args[1]);
            } catch (NumberFormatException e) {
                System.out.println("Usage: /<nickname>:<serverId> <file path>");
            }
        } else {
            client.sendPublicMessage(line);
        }

    }
}
