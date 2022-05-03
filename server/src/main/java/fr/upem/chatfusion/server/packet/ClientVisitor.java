package fr.upem.chatfusion.server.packet;

import fr.upem.chatfusion.common.context.Context;
import fr.upem.chatfusion.common.packet.FileChunk;
import fr.upem.chatfusion.common.packet.MsgPbl;
import fr.upem.chatfusion.common.packet.MsgPrv;
import fr.upem.chatfusion.common.packet.PacketVisitor;
import fr.upem.chatfusion.server.PeerContext;
import fr.upem.chatfusion.server.Server;

import java.util.Objects;

public class ClientVisitor implements PacketVisitor {

    private final Server server;
    private final PeerContext context;

    public ClientVisitor(Server server, PeerContext context) {
        this.server = server;
        this.context = context;
    }

    @Override
    public void visit(MsgPbl packet) {
        Objects.requireNonNull(packet);
        if (packet.nickname().equals(context.getNickname())) {
            server.dispatchPublicMessage(packet, true);
        }
    }

    @Override
    public void visit(MsgPrv packet) {
        Objects.requireNonNull(packet);
        if (packet.srcNickname().equals(context.getNickname())) {
            server.dispatchPrivateMessage(packet);
        }
    }

    @Override
    public void visit(FileChunk packet) {
        Objects.requireNonNull(packet);
        PacketVisitor.super.visit(packet);
    }
}
