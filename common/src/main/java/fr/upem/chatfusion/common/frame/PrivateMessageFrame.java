package fr.upem.chatfusion.common.frame;

public record PrivateMessageFrame(int serverId, String nickname, String message) {
}
