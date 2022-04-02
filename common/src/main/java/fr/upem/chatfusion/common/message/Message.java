package fr.upem.chatfusion.common.message;

public record Message(int serverId, String senderNickname, String message) {

    @Override
    public String toString() {
        return "[S-%d] %s: %s".formatted(serverId, senderNickname, message);
    }
}
