package legacyconnector.v1_6_4.net.minecraft.network.packet;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import legacyconnector.v1_7_10.net.minecraft.network.Packet;
import net.minecraft.server.level.ServerPlayer;

/** 1.6.4's destroy entities packet: a count byte and the entity ids. */
public class Packet29DestroyEntity extends Packet {
    public int[] entityId = new int[0];

    public Packet29DestroyEntity() {}

    public Packet29DestroyEntity(int... ids) {
        this.entityId = ids;
    }

    @Override
    public void readPacketData(DataInput in) throws IOException {
        entityId = new int[in.readByte() & 255];
        for (int i = 0; i < entityId.length; i++) entityId[i] = in.readInt();
    }

    @Override
    public void writePacketData(DataOutput out) throws IOException {
        out.writeByte(entityId.length);
        for (int id : entityId) out.writeInt(id);
    }

    @Override
    public int getPacketSize() {
        return 1 + entityId.length * 4;
    }

    @Override
    public void mcc$send(ServerPlayer player) {
        player.connection.send(new net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket(entityId));
    }
}
