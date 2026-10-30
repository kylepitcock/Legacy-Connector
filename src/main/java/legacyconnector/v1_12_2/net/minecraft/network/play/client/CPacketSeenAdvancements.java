package legacyconnector.v1_12_2.net.minecraft.network.play.client;

import java.io.IOException;
import legacyconnector.v1_12_2.net.minecraft.network.Packet;
import legacyconnector.v1_12_2.net.minecraft.network.PacketBuffer;
import legacyconnector.v1_12_2.net.minecraft.network.play.INetHandlerPlayServer;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;

/** The advancements screen opened a tab or closed (sent as the game's seen advancements packet). */
public class CPacketSeenAdvancements implements Packet<INetHandlerPlayServer> {
    private Action action;
    private ResourceLocation tab;

    public CPacketSeenAdvancements() {
    }

    public CPacketSeenAdvancements(Action action, ResourceLocation tab) {
        this.action = action;
        this.tab = tab;
    }

    public static CPacketSeenAdvancements openedTab(legacyconnector.v1_12_2.net.minecraft.advancements.Advancement advancement) {
        return new CPacketSeenAdvancements(Action.OPENED_TAB, advancement.getId());
    }

    public static CPacketSeenAdvancements closedScreen() {
        return new CPacketSeenAdvancements(Action.CLOSED_SCREEN, null);
    }

    public Action getAction() {
        return action;
    }

    public ResourceLocation getTab() {
        return tab;
    }

    @Override
    public void readPacketData(PacketBuffer buf) throws IOException {
        action = buf.readEnumValue(Action.class);
        if (action == Action.OPENED_TAB) tab = new ResourceLocation(buf.readString(32767));
    }

    @Override
    public void writePacketData(PacketBuffer buf) throws IOException {
        buf.writeEnumValue(action);
        if (action == Action.OPENED_TAB) buf.writeString(tab.toString());
    }

    @Override
    public void processPacket(INetHandlerPlayServer handler) {
    }

    @Override
    public net.minecraft.network.protocol.Packet<?> mcc$modern() {
        return action == Action.OPENED_TAB
                ? new net.minecraft.network.protocol.game.ServerboundSeenAdvancementsPacket(
                        net.minecraft.network.protocol.game.ServerboundSeenAdvancementsPacket.Action.OPENED_TAB, tab.mcc$modern())
                : net.minecraft.network.protocol.game.ServerboundSeenAdvancementsPacket.closedScreen();
    }

    public enum Action {
        OPENED_TAB,
        CLOSED_SCREEN
    }
}
