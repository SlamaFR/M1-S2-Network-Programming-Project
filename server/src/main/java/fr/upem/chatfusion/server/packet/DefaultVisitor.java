package fr.upem.chatfusion.server.packet;

import fr.upem.chatfusion.common.packet.AuthGst;
import fr.upem.chatfusion.common.packet.FusionAckLeader;
import fr.upem.chatfusion.common.packet.FusionInit;
import fr.upem.chatfusion.common.packet.PacketVisitor;
import fr.upem.chatfusion.server.Server;

import java.nio.channels.SelectionKey;

public class DefaultVisitor implements PacketVisitor {

    private final Server server;
    private final SelectionKey key;

    public DefaultVisitor(Server server, SelectionKey key) {
        this.server = server;
        this.key = key;
    }

    @Override
    public void visit(AuthGst packet) {
        server.authenticateGuest(packet.username(), key);
    }

    @Override
    public void visit(FusionInit packet) {
        server.initIncomingFusion(packet, key);
    }

    @Override
    public void visit(FusionAckLeader packet) {
        server.handleFusionAckLeader(packet, key);
    }
}
