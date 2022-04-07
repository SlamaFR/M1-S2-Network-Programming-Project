package fr.upem.chatfusion.client;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Path;

public class Application {

    public static void main(String[] args) throws NumberFormatException, IOException {
        if (args.length != 4) {
            usage();
            return;
        }
        new Client(args[0], new InetSocketAddress(args[2], Integer.parseInt(args[3])), Path.of(args[1])).launch();
    }

    private static void usage() {
        System.err.println("Usage: ChatFusionServer <Nickname> <Path> <Hostname> <Port>");
    }

}