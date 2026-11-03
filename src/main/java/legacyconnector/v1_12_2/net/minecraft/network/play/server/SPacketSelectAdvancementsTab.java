package legacyconnector.v1_12_2.net.minecraft.network.play.server;

import java.io.IOException;
import legacyconnector.v1_12_2.net.minecraft.network.Packet;
import legacyconnector.v1_12_2.net.minecraft.network.PacketBuffer;
import legacyconnector.v1_12_2.net.minecraft.network.play.INetHandlerPlayClient;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;

/** The server selects a tab of the advancements screen. */
public class SPacketSelectAdvancementsTab implements Packet<INetHandlerPlayClient> {
    private ResourceLocation tab;

    public SPacketSelectAdvancementsTab() {
    }

    public SPacketSelectAdvancementsTab(ResourceLocation tab) {
        this.tab = tab;
    }

    public ResourceLocation getTab() {
        return tab;
    }

    @Override
    public void readPacketData(PacketBuffer buf) throws IOException {
        tab = buf.readBoolean() ? buf.readResourceLocation() : null;
    }

    @Override
    public void writePacketData(PacketBuffer buf) throws IOException {
        buf.writeBoolean(tab != null);
        if (tab != null) buf.writeResourceLocation(tab);
    }

    @Override
    public void processPacket(INetHandlerPlayClient handler) {
    }

    @Override
    public net.minecraft.network.protocol.Packet<?> mcc$modern() {
        return new net.minecraft.network.protocol.game.ClientboundSelectAdvancementsTabPacket(tab == null ? null : tab.mcc$modern());
    }
}
