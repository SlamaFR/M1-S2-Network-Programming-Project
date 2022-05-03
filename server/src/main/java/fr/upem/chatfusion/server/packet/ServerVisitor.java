package fr.upem.chatfusion.server.packet;

import fr.upem.chatfusion.common.packet.FusionChangeLeader;
import fr.upem.chatfusion.common.packet.FusionInitFwd;
import fr.upem.chatfusion.common.packet.FusionInitKo;
import fr.upem.chatfusion.common.packet.FusionInitOk;
import fr.upem.chatfusion.common.packet.FusionReq;
import fr.upem.chatfusion.common.packet.FusionRsp;
import fr.upem.chatfusion.common.packet.MsgPbl;
import fr.upem.chatfusion.common.packet.MsgPrv;
import fr.upem.chatfusion.common.packet.PacketVisitor;
import fr.upem.chatfusion.server.PeerContext;
import fr.upem.chatfusion.server.Server;

import java.io.IOException;
import java.util.Objects;

public class ServerVisitor implements PacketVisitor {

    private final Server server;
    private final PeerContext context;

    public ServerVisitor(Server server, PeerContext context) {
        this.server = server;
        this.context = context;
    }

    @Override
    public void visit(MsgPbl packet) {
        Objects.requireNonNull(packet);
        server.dispatchPublicMessage(packet, false);
    }

    @Override
    public void visit(MsgPrv packet) {
        Objects.requireNonNull(packet);
        server.dispatchPrivateMessage(packet);
    }

    @Override
    public void visit(FusionInitOk packet) {
        Objects.requireNonNull(packet);
        server.handleInitOk(packet, context);
    }

    @Override
    public void visit(FusionInitKo packet) {
        System.out.println("Fusion rejected");
    }

    @Override
    public void visit(FusionReq packet) {
        server.handleFusionReq(packet, context);
    }

    @Override
    public void visit(FusionInitFwd packet) {
        Objects.requireNonNull(packet);
        System.out.println("Server leader is " + packet.address());
        System.out.println("Forwarding fusion init...");
        server.releaseFusionLockIfPossible();
        try {
            server.initOutgoingFusion(packet.address());
        } catch (InterruptedException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public void visit(FusionRsp packet) {
        System.out.println("Fusion response received: " + packet.status());
        server.releaseFusionLockIfPossible();
    }

    @Override
    public void visit(FusionChangeLeader packet) {
        Objects.requireNonNull(packet);
        System.out.println("Server leader is " + packet.address());
        server.switchLeader(packet.address());
    }
}
