package fr.upem.chatfusion.client;

import fr.upem.chatfusion.common.packet.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Objects;

public class ServerVisitor implements PacketVisitor {

    private final Client client;
    private final HashMap<FileReceiver, Integer> currentFilesReceiving;

    public ServerVisitor(Client client) {
        Objects.requireNonNull(client);
        this.client = Objects.requireNonNull(client);
        this.currentFilesReceiving = new HashMap<>();
    }

    @Override
    public void visit(AuthRsp packet) {
        Objects.requireNonNull(packet);
        switch (packet.code()) {
            case AuthRsp.OK -> {
                if (packet.serverId() <= 0) {
                    System.out.println("Invalid server ID");
                    client.shutdown();
                    return;
                }
                client.confirmAuthentication(packet.serverId());
            }
            case AuthRsp.NICKNAME_IN_USE -> {
                System.out.println("Nickname already in use");
                client.shutdown();
            }
        }
    }

    @Override
    public void visit(MsgPbl packet) {
        Objects.requireNonNull(packet);
        System.out.printf("[%d] %s: %s\n", packet.serverId(), packet.nickname(), packet.message());
    }

    @Override
    public void visit(MsgPrv packet) {
        Objects.requireNonNull(packet);
        System.out.printf("[%d] %s to you: %s\n", packet.srcServerId(), packet.srcNickname(), packet.message());
    }

    public void visit(FileChunk packet) {
        Objects.requireNonNull(packet);
        var key = new FileReceiver(packet.transferID(), packet.srcNickname(), packet.dstNickname(), packet.filename());
        currentFilesReceiving.computeIfAbsent(key, k -> k.createFile() ? 0 :-1);
        var numberOfChunksReceive = currentFilesReceiving.compute(key, (k, v) -> {
            if (v == -1) {
                return -1;
            }
            return k.writeChunk(packet.chunk()) ? ++v : -1;
        });
        if (numberOfChunksReceive == -1) {
            System.out.println("Erreur lors de la creation du fichier ! ");
            return ;
        }
        if (numberOfChunksReceive == packet.chunkNumber()) {
            currentFilesReceiving.remove(key);
            System.out.printf("[%d] %s received from %s\n", packet.srcServerId(), packet.filename(), packet.srcNickname());
        }


    }
}
