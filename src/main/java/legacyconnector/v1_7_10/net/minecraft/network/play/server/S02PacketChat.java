package legacyconnector.v1_7_10.net.minecraft.network.play.server;

import java.io.IOException;
import legacyconnector.v1_7_10.net.minecraft.network.Packet;
import legacyconnector.v1_7_10.net.minecraft.network.PacketBuffer;

/** A chat line for the player. */
public class S02PacketChat extends Packet {
    public legacyconnector.v1_7_10.net.minecraft.util.IChatComponent chatComponent;
    public boolean isChat;

    public S02PacketChat() {
    }

    public S02PacketChat(legacyconnector.v1_7_10.net.minecraft.util.IChatComponent component) {
        this(component, true);
    }

    public S02PacketChat(legacyconnector.v1_7_10.net.minecraft.util.IChatComponent component, boolean isChat) {
        this.chatComponent = component;
        this.isChat = isChat;
    }

    @Override
    public void readPacketData(PacketBuffer data) throws IOException {
        chatComponent = legacyconnector.v1_7_10.net.minecraft.util.IChatComponent.Serializer.jsonToComponent(data.readStringFromBuffer(32767));
    }

    @Override
    public void writePacketData(PacketBuffer data) throws IOException {
        data.writeStringToBuffer(legacyconnector.v1_7_10.net.minecraft.util.IChatComponent.Serializer.componentToJson(chatComponent));
    }

    public legacyconnector.v1_7_10.net.minecraft.util.IChatComponent func_148915_c() {
        return chatComponent;
    }

    public boolean isChat() {
        return isChat;
    }

    @Override
    public void mcc$send(net.minecraft.server.level.ServerPlayer player) {
        if (chatComponent != null) player.sendSystemMessage(chatComponent.mcc$toModern());
    }
}
