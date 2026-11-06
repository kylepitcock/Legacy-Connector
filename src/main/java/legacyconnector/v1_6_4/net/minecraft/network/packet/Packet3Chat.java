package legacyconnector.v1_6_4.net.minecraft.network.packet;

import legacyconnector.v1_6_4.net.minecraft.util.ChatMessageComponent;
import legacyconnector.v1_7_10.net.minecraft.network.Packet;
import net.minecraft.server.level.ServerPlayer;

/** 1.6.4's chat packet: sent to a player it shows the message in their chat, sent to the server it is what they typed. */
public class Packet3Chat extends Packet {
    public String message;
    public boolean isServer = true;
    private ChatMessageComponent component;

    public Packet3Chat() {}

    public Packet3Chat(String message) {
        this.message = message;
    }

    public Packet3Chat(ChatMessageComponent component) {
        this.component = component;
        this.message = component.toString();
    }

    @Override
    public void mcc$send(ServerPlayer player) {
        if (component != null) {
            player.sendSystemMessage(component.mcc$toLegacy().mcc$toModern());
        } else if (message != null) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal(message));
        }
    }

    @Override
    public void mcc$sendToServer() {
        var connection = net.minecraft.client.Minecraft.getInstance().getConnection();
        if (connection == null || message == null) return;
        if (message.startsWith("/")) connection.sendCommand(message.substring(1));
        else connection.sendChat(message);
    }
}
