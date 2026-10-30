package legacyconnector.v1_12_2.net.minecraft.network.play.client;

import java.io.IOException;
import legacyconnector.v1_12_2.net.minecraft.network.Packet;
import legacyconnector.v1_12_2.net.minecraft.network.PacketBuffer;
import legacyconnector.v1_12_2.net.minecraft.network.play.INetHandlerPlayServer;

/** The selected hotbar slot (sent as the game's carried item packet). */
public class CPacketHeldItemChange implements Packet<INetHandlerPlayServer> {
    private int slotId;

    public CPacketHeldItemChange() {
    }

    public CPacketHeldItemChange(int slot) {
        this.slotId = slot;
    }

    public int getSlotId() {
        return slotId;
    }

    @Override
    public void readPacketData(PacketBuffer buf) throws IOException {
        slotId = buf.readShort();
    }

    @Override
    public void writePacketData(PacketBuffer buf) throws IOException {
        buf.writeShort(slotId);
    }

    @Override
    public void processPacket(INetHandlerPlayServer handler) {
    }

    @Override
    public net.minecraft.network.protocol.Packet<?> mcc$modern() {
        return new net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket(slotId);
    }
}
