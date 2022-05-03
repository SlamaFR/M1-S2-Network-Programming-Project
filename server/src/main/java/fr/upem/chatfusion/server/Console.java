package fr.upem.chatfusion.server;

import java.net.InetSocketAddress;

public class Console implements Runnable {

    private final Server server;

    public Console(Server server) {
        this.server = server;
    }

    @Override
    public void run() {
        try (var scanner = new java.util.Scanner(System.in)) {
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
        if (line.toUpperCase().startsWith("FUSION")) {
            try {
                var args = line.split(" ", 3);
                if (args.length < 3) {
                    System.out.println("Usage: fusion <host> <port>");
                    return;
                }
                var address = new InetSocketAddress(args[1], Integer.parseInt(args[2]));
                if (address.isUnresolved()) {
                    System.out.println("Unresolved host");
                    return;
                }
                server.initOutgoingFusion(address);
            } catch (NumberFormatException e) {
                System.out.println("Usage: fusion <address> <port>");
            }
        } else if ("INFO".equalsIgnoreCase(line)) {
            System.out.println("Server info");
            System.out.println(">> Neighbors: " + server.getNeighbors());
        } else if ("SHUTDOWN".equalsIgnoreCase(line)) {
            System.out.println(">> Server shutdown");
        } else if ("SHUTDOWNNOW".equalsIgnoreCase(line)) {
            System.out.println(">> Server shutdown now");
        }
    }
}
