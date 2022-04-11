package fr.upem.chatfusion.client;

import fr.upem.chatfusion.common.packet.AuthRsp;
import fr.upem.chatfusion.common.packet.MsgPbl;
import fr.upem.chatfusion.common.packet.MsgPrv;
import fr.upem.chatfusion.common.packet.PacketVisitor;

import java.util.Objects;

public class ServerVisitor implements PacketVisitor {

    private final Client client;

    public ServerVisitor(Client client) {
        Objects.requireNonNull(client);
        this.client = Objects.requireNonNull(client);
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
}
