package legacyconnector.v1_7_10.net.minecraft.network.play.server;

import java.io.IOException;
import legacyconnector.v1_7_10.net.minecraft.network.Packet;
import legacyconnector.v1_7_10.net.minecraft.network.PacketBuffer;

/** The client forgets these entities (they left its range or died). */
public class S13PacketDestroyEntities extends Packet {
    private int[] entityIds = new int[0];

    public S13PacketDestroyEntities() {
    }

    public S13PacketDestroyEntities(int... entityIds) {
        this.entityIds = entityIds;
    }

    @Override
    public void readPacketData(PacketBuffer data) throws IOException {
        entityIds = new int[data.readByte()];
        for (int i = 0; i < entityIds.length; i++) entityIds[i] = data.readInt();
    }

    @Override
    public void writePacketData(PacketBuffer data) throws IOException {
        data.writeByte(entityIds.length);
        for (int id : entityIds) data.writeInt(id);
    }

    public int[] func_149098_c() {
        return entityIds;
    }

    @Override
    public void mcc$send(net.minecraft.server.level.ServerPlayer player) {
        player.connection.send(new net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket(entityIds));
    }
}
