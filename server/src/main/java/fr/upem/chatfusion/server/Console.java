package fr.upem.chatfusion.server;

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
        }
    }

    private void processLine(String line) {
        switch (line.toUpperCase()) {
            case "INFO" -> {
                System.out.println(">> Server info");
            }
            case "SHUTDOWN" -> {
                System.out.println(">> Server shutdown");
            }
            case "SHUTDOWNNOW" -> {
                System.out.println(">> Server shutdown now");
            }
        }
    }
}
