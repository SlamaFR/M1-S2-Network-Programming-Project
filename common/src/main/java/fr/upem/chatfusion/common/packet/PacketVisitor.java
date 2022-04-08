package fr.upem.chatfusion.common.packet;

public interface PacketVisitor {

    default void visit(AuthGst packet) {
        throw new UnsupportedOperationException();
    }

    default void visit(AuthUsr packet) {
        throw new UnsupportedOperationException();
    }

    default void visit(AuthRsp packet) {
        throw new UnsupportedOperationException();
    }

    default void visit(MsgPbl packet) {
        throw new UnsupportedOperationException();
    }

    default void visit(MsgPrv packet) {
        throw new UnsupportedOperationException();
    }

    default void visit(FileChunk packet) {
        throw new UnsupportedOperationException();
    }

    default void visit(FusionInit packet) {
        throw new UnsupportedOperationException();
    }

    default void visit(FusionInitOk packet) {
        throw new UnsupportedOperationException();
    }

    default void visit(FusionInitKo packet) {
        throw new UnsupportedOperationException();
    }

    default void visit(FusionInitFwd packet) {
        throw new UnsupportedOperationException();
    }

    default void visit(FusionReq packet) {
        throw new UnsupportedOperationException();
    }

    default void visit(FusionRsp packet) {
        throw new UnsupportedOperationException();
    }

    default void visit(FusionChangeLeader packet) {
        throw new UnsupportedOperationException();
    }

    default void visit(FusionAckLeader packet) {
        throw new UnsupportedOperationException();
    }
}
