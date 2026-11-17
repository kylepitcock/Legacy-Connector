package legacyconnector.v1_6_4.net.minecraft.network.packet;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import legacyconnector.v1_7_10.net.minecraft.entity.Entity;
import legacyconnector.v1_7_10.net.minecraft.network.Packet;
import net.minecraft.server.level.ServerPlayer;

/** 1.6.4's global entity packet (lightning bolts; positions in 1/32 blocks): delivered as the modern add-entity packet of a lightning bolt. */
public class Packet71Weather extends Packet {
    public int entityID;
    public int posX;
    public int posY;
    public int posZ;
    public int isLightningBolt;
    private net.minecraft.world.entity.Entity mcc$entity;

    public Packet71Weather() {}

    public Packet71Weather(Entity entity) {
        this.entityID = entity.getEntityId();
        this.mcc$entity = entity.mcc$entity();
        this.posX = (int) Math.floor(mcc$entity.getX() * 32.0D);
        this.posY = (int) Math.floor(mcc$entity.getY() * 32.0D);
        this.posZ = (int) Math.floor(mcc$entity.getZ() * 32.0D);
        this.isLightningBolt = mcc$entity instanceof net.minecraft.world.entity.LightningBolt ? 1 : 0;
    }

    @Override
    public void readPacketData(DataInput in) throws IOException {
        entityID = in.readInt();
        isLightningBolt = in.readByte();
        posX = in.readInt();
        posY = in.readInt();
        posZ = in.readInt();
    }

    @Override
    public void writePacketData(DataOutput out) throws IOException {
        out.writeInt(entityID);
        out.writeByte(isLightningBolt);
        out.writeInt(posX);
        out.writeInt(posY);
        out.writeInt(posZ);
    }

    @Override
    public int getPacketSize() {
        return 17;
    }

    @Override
    public void mcc$send(ServerPlayer player) {
        if (isLightningBolt != 1) return;
        net.minecraft.world.entity.Entity bolt = mcc$entity;
        if (bolt == null) {
            bolt = net.minecraft.world.entity.EntityType.LIGHTNING_BOLT.create(player.serverLevel());
            if (bolt == null) return;
            bolt.setId(entityID);
            bolt.moveTo(posX / 32.0D, posY / 32.0D, posZ / 32.0D);
        }
        player.connection.send(new net.minecraft.network.protocol.game.ClientboundAddEntityPacket(bolt, 0, bolt.blockPosition()));
    }
}
