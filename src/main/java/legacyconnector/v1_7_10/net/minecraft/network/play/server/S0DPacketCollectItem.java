package legacyconnector.v1_7_10.net.minecraft.network.play.server;

import java.io.IOException;
import legacyconnector.v1_7_10.net.minecraft.network.Packet;
import legacyconnector.v1_7_10.net.minecraft.network.PacketBuffer;

/** An entity picks up an item entity (the fly-to-player animation and sound). */
public class S0DPacketCollectItem extends Packet {
    private int collectedItemId;
    private int collectorId;

    public S0DPacketCollectItem() {
    }

    public S0DPacketCollectItem(int collectedItemId, int collectorId) {
        this.collectedItemId = collectedItemId;
        this.collectorId = collectorId;
    }

    @Override
    public void readPacketData(PacketBuffer data) throws IOException {
        collectedItemId = data.readInt();
        collectorId = data.readInt();
    }

    @Override
    public void writePacketData(PacketBuffer data) throws IOException {
        data.writeInt(collectedItemId);
        data.writeInt(collectorId);
    }

    public int func_149354_c() {
        return collectedItemId;
    }

    public int func_149353_d() {
        return collectorId;
    }

    @Override
    public void mcc$send(net.minecraft.server.level.ServerPlayer player) {
        player.connection.send(new net.minecraft.network.protocol.game.ClientboundTakeItemEntityPacket(collectedItemId, collectorId, 1));
    }
}
