package fr.upem.chatfusion.server;

import java.io.IOException;
import java.util.logging.Logger;

public class Application {

    private static final Logger LOGGER = Logger.getLogger(Application.class.getName());

    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            usage();
            return;
        }
        try {
            new Server(Integer.parseInt(args[1]), Integer.parseInt(args[0])).launch();
            LOGGER.info("Server stopped.");
        } catch (NumberFormatException e) {
            usage();
        }
    }

    private static void usage() {
        System.err.println("Usage: ChatFusionServer <Server ID> <Port>");
    }

}
