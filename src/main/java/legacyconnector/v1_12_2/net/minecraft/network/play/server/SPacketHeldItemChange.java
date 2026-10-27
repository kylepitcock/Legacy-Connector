package legacyconnector.v1_12_2.net.minecraft.network.play.server;

import java.io.IOException;
import legacyconnector.v1_12_2.net.minecraft.network.Packet;
import legacyconnector.v1_12_2.net.minecraft.network.PacketBuffer;
import legacyconnector.v1_12_2.net.minecraft.network.play.INetHandlerPlayClient;

/** Selects the player's hotbar slot. */
public class SPacketHeldItemChange implements Packet<INetHandlerPlayClient> {
    private int heldItemHotbarIndex;

    public SPacketHeldItemChange() {
    }

    public SPacketHeldItemChange(int hotbarIndexIn) {
        this.heldItemHotbarIndex = hotbarIndexIn;
    }

    public int getHeldItemHotbarIndex() {
        return heldItemHotbarIndex;
    }

    @Override
    public void readPacketData(PacketBuffer buf) throws IOException {
        heldItemHotbarIndex = buf.readByte();
    }

    @Override
    public void writePacketData(PacketBuffer buf) throws IOException {
        buf.writeByte(heldItemHotbarIndex);
    }

    /** Handled by the modern client as the modern packet. */
    @Override
    public void processPacket(INetHandlerPlayClient handler) {
    }

    @Override
    public net.minecraft.network.protocol.Packet<?> mcc$modern() {
        return new net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket(heldItemHotbarIndex);
    }
}
