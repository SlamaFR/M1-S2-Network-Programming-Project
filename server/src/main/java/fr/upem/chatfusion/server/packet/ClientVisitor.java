package fr.upem.chatfusion.server.packet;

import fr.upem.chatfusion.common.packet.FileChunk;
import fr.upem.chatfusion.common.packet.MsgPbl;
import fr.upem.chatfusion.common.packet.MsgPrv;
import fr.upem.chatfusion.common.packet.PacketVisitor;
import fr.upem.chatfusion.server.Server;

public class ClientVisitor implements PacketVisitor {

    private final Server server;

    public ClientVisitor(Server server) {
        this.server = server;
    }

    @Override
    public void visit(MsgPbl packet) {
        server.dispatchPublicMessage(packet);
    }

    @Override
    public void visit(MsgPrv packet) {
        server.dispatchPrivateMessage(packet);
    }

    @Override
    public void visit(FileChunk packet) {
        PacketVisitor.super.visit(packet);
    }
}
