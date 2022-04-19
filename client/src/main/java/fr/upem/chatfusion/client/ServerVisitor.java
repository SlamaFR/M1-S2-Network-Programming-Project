package fr.upem.chatfusion.client;

import fr.upem.chatfusion.common.packet.*;

import java.io.File;
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
        var key = new FileReceiver(packet.transferID(), packet.srcNickname());
        currentFilesReceiving.putIfAbsent(key, 0);
        var numberOfChunksReceive = currentFilesReceiving.compute(key, (k, v) -> ++v);
        if (numberOfChunksReceive == packet.chunkNumber()) {
            currentFilesReceiving.remove(key);
            System.out.printf("[%d] %s received from %s\n", packet.serverId(), packet.filename(), packet.srcNickname());
        }
    }
}
