package legacyconnector.v1_12_2.net.minecraft.network.play.server;

import java.io.IOException;
import legacyconnector.v1_12_2.net.minecraft.network.Packet;
import legacyconnector.v1_12_2.net.minecraft.network.PacketBuffer;
import legacyconnector.v1_12_2.net.minecraft.network.play.INetHandlerPlayClient;

/** A chat message, system message or action bar text. */
public class SPacketChat implements Packet<INetHandlerPlayClient> {
    private legacyconnector.v1_12_2.net.minecraft.util.text.ITextComponent chatComponent;
    private legacyconnector.v1_12_2.net.minecraft.util.text.ChatType type;

    public SPacketChat() {
    }

    public SPacketChat(legacyconnector.v1_12_2.net.minecraft.util.text.ITextComponent componentIn) {
        this(componentIn, legacyconnector.v1_12_2.net.minecraft.util.text.ChatType.SYSTEM);
    }

    public SPacketChat(legacyconnector.v1_12_2.net.minecraft.util.text.ITextComponent message, legacyconnector.v1_12_2.net.minecraft.util.text.ChatType type) {
        this.chatComponent = message;
        this.type = type;
    }

    public legacyconnector.v1_12_2.net.minecraft.util.text.ITextComponent getChatComponent() {
        return chatComponent;
    }

    public boolean isSystem() {
        return type == legacyconnector.v1_12_2.net.minecraft.util.text.ChatType.SYSTEM || type == legacyconnector.v1_12_2.net.minecraft.util.text.ChatType.GAME_INFO;
    }

    public legacyconnector.v1_12_2.net.minecraft.util.text.ChatType getType() {
        return type;
    }

    @Override
    public void readPacketData(PacketBuffer buf) throws IOException {
        chatComponent = buf.readTextComponent();
        type = legacyconnector.v1_12_2.net.minecraft.util.text.ChatType.byId(buf.readByte());
    }

    @Override
    public void writePacketData(PacketBuffer buf) throws IOException {
        buf.writeTextComponent(chatComponent);
        buf.writeByte(type.getId());
    }

    /** Handled by the modern client as the modern packet. */
    @Override
    public void processPacket(INetHandlerPlayClient handler) {
    }

    @Override
    public net.minecraft.network.protocol.Packet<?> mcc$modern() {
        return new net.minecraft.network.protocol.game.ClientboundSystemChatPacket(
                chatComponent == null ? net.minecraft.network.chat.Component.empty() : chatComponent.mcc$modern(),
                type == legacyconnector.v1_12_2.net.minecraft.util.text.ChatType.GAME_INFO);
    }
}
