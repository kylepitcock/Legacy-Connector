package legacyconnector.v1_12_2.net.minecraft.network.play.server;

import java.io.IOException;
import legacyconnector.v1_12_2.net.minecraft.network.Packet;
import legacyconnector.v1_12_2.net.minecraft.network.PacketBuffer;
import legacyconnector.v1_12_2.net.minecraft.network.play.INetHandlerPlayClient;

/**
 * An advancement update for the client. The legacy payload (advancement builders, progress maps) has no counterpart in the shim, so a
 * packet made by a mod is an empty update (it resets nothing and adds nothing); the game sends its own updates.
 */
public class SPacketAdvancementInfo implements Packet<INetHandlerPlayClient> {
    private boolean firstSync;

    public SPacketAdvancementInfo() {
    }

    public SPacketAdvancementInfo(boolean firstSync) {
        this.firstSync = firstSync;
    }

    @Override
    public void readPacketData(PacketBuffer buf) throws IOException {
        firstSync = buf.readBoolean();
    }

    @Override
    public void writePacketData(PacketBuffer buf) throws IOException {
        buf.writeBoolean(firstSync);
    }

    @Override
    public void processPacket(INetHandlerPlayClient handler) {
    }

    @Override
    public net.minecraft.network.protocol.Packet<?> mcc$modern() {
        return new net.minecraft.network.protocol.game.ClientboundUpdateAdvancementsPacket(firstSync, java.util.List.of(), java.util.Set.of(), java.util.Map.of());
    }
}
