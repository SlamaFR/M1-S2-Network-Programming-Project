package fr.upem.chatfusion.client;

import java.io.IOException;
import java.net.InetSocketAddress;

public class Application {

    public static void main(String[] args) throws NumberFormatException, IOException {
        if (args.length != 2) {
            usage();
            return;
        }
        new Client(new InetSocketAddress(args[0], Integer.parseInt(args[1]))).launch();
    }

    private static void usage() {
        System.err.println("Usage: ChatFusionServer <Nickname> <Hostname> <Port>");
    }

}