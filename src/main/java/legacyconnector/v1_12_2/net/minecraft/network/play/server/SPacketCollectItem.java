package legacyconnector.v1_12_2.net.minecraft.network.play.server;

import java.io.IOException;
import legacyconnector.v1_12_2.net.minecraft.network.Packet;
import legacyconnector.v1_12_2.net.minecraft.network.PacketBuffer;
import legacyconnector.v1_12_2.net.minecraft.network.play.INetHandlerPlayClient;

/** An item (or arrow, or orb) flying into the entity that picked it up. */
public class SPacketCollectItem implements Packet<INetHandlerPlayClient> {
    private int collectedItemEntityId;
    private int entityId;
    private int collectedQuantity;

    public SPacketCollectItem() {
    }

    public SPacketCollectItem(int collectedItemEntityIdIn, int entityIdIn, int collectedQuantityIn) {
        this.collectedItemEntityId = collectedItemEntityIdIn;
        this.entityId = entityIdIn;
        this.collectedQuantity = collectedQuantityIn;
    }

    public int mcc$collectedItemEntityId() {
        return collectedItemEntityId;
    }

    public int mcc$entityId() {
        return entityId;
    }

    public int mcc$collectedQuantity() {
        return collectedQuantity;
    }


    @Override
    public void readPacketData(PacketBuffer buf) throws IOException {
        // read by the modern client from the modern packet
    }

    @Override
    public void writePacketData(PacketBuffer buf) throws IOException {
        // written as the modern packet
    }

    /** Handled by the modern client as the modern packet. */
    @Override
    public void processPacket(INetHandlerPlayClient handler) {
    }

    @Override
    public net.minecraft.network.protocol.Packet<?> mcc$modern() {
        return new net.minecraft.network.protocol.game.ClientboundTakeItemEntityPacket(collectedItemEntityId, entityId, collectedQuantity);
    }
}
