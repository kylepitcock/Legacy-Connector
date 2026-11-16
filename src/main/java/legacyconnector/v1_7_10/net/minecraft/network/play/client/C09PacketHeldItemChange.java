package legacyconnector.v1_7_10.net.minecraft.network.play.client;

import java.io.IOException;
import legacyconnector.v1_7_10.net.minecraft.network.Packet;
import legacyconnector.v1_7_10.net.minecraft.network.PacketBuffer;

/** The client selected a hotbar slot (0-8). */
public class C09PacketHeldItemChange extends Packet {
    private int slotId;

    public C09PacketHeldItemChange() {
    }

    public C09PacketHeldItemChange(int slotId) {
        this.slotId = slotId;
    }

    @Override
    public void readPacketData(PacketBuffer data) throws IOException {
        slotId = data.readShort();
    }

    @Override
    public void writePacketData(PacketBuffer data) throws IOException {
        data.writeShort(slotId);
    }

    public int getSlotId() {
        return slotId;
    }

    @Override
    public void mcc$sendToServer() {
        if (slotId >= 0 && slotId < 9) {
            org.kylepitcock.legacyconnector.v1_7_10.client.ClientNetwork1710.send(new net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket(slotId));
        }
    }
}
