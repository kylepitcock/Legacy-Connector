package legacyconnector.v1_6_4.net.minecraft.network.packet;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.List;
import legacyconnector.v1_6_4.forward.Packets164;
import legacyconnector.v1_7_10.net.minecraft.entity.DataWatcher;
import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraft.network.Packet;
import net.minecraft.server.level.ServerPlayer;

/**
 * 1.6.4's player spawn packet: entity id, name, position in 1/32 blocks, rotation and pitch in 1/256 turns, the held item's id and the watched
 * values. Built from a player it carries that player's state; delivered it makes the modern game add that player's entity for the client.
 */
public class Packet20NamedEntitySpawn extends Packet {
    public int entityId;
    public String name = "";
    public int xPosition;
    public int yPosition;
    public int zPosition;
    public byte rotation;
    public byte pitch;
    public int currentItem;
    private DataWatcher metadata;
    private List<DataWatcher.WatchableObject> metadataList;
    private net.minecraft.world.entity.Entity mcc$entity;

    public Packet20NamedEntitySpawn() {}

    public Packet20NamedEntitySpawn(EntityPlayer player) {
        this.entityId = player.getEntityId();
        this.name = player.getCommandSenderName();
        this.mcc$entity = player.mcc$entity();
        this.xPosition = (int) Math.floor(mcc$entity.getX() * 32.0D);
        this.yPosition = (int) Math.floor(mcc$entity.getY() * 32.0D);
        this.zPosition = (int) Math.floor(mcc$entity.getZ() * 32.0D);
        this.rotation = (byte) (int) (mcc$entity.getYRot() * 256.0F / 360.0F);
        this.pitch = (byte) (int) (mcc$entity.getXRot() * 256.0F / 360.0F);
        legacyconnector.v1_7_10.net.minecraft.item.ItemStack held = player.getHeldItem();
        this.currentItem = held == null ? 0 : legacyconnector.v1_6_4.net.minecraft.util.Ids.itemId(held.getItem());
        this.metadata = player.getDataWatcher();
    }

    /** The watched values: those of the player, or the ones read from the wire. */
    public List<DataWatcher.WatchableObject> getWatchedMetadata() {
        if (metadataList == null && metadata != null) metadataList = metadata.getAllWatched();
        return metadataList;
    }

    @Override
    public void readPacketData(DataInput in) throws IOException {
        entityId = in.readInt();
        name = Packets164.readString(in, 16);
        xPosition = in.readInt();
        yPosition = in.readInt();
        zPosition = in.readInt();
        rotation = in.readByte();
        pitch = in.readByte();
        currentItem = in.readShort();
        metadataList = legacyconnector.v1_6_4.facade.net.minecraft.entity.DataWatcher.readWatchableObjects(in);
    }

    @Override
    public void writePacketData(DataOutput out) throws IOException {
        out.writeInt(entityId);
        Packets164.writeString(name, out);
        out.writeInt(xPosition);
        out.writeInt(yPosition);
        out.writeInt(zPosition);
        out.writeByte(rotation);
        out.writeByte(pitch);
        out.writeShort(currentItem);
        legacyconnector.v1_6_4.facade.net.minecraft.entity.DataWatcher.writeObjectsInListToStream(getWatchedMetadata(), out);
    }

    @Override
    public int getPacketSize() {
        return 28;
    }

    @Override
    public void mcc$send(ServerPlayer player) {
        if (mcc$entity == null || mcc$entity == player) return;
        player.connection.send(new net.minecraft.network.protocol.game.ClientboundAddEntityPacket(mcc$entity, 0, mcc$entity.blockPosition()));
    }
}
