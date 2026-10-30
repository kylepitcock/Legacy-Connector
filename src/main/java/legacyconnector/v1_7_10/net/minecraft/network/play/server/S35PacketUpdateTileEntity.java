package legacyconnector.v1_7_10.net.minecraft.network.play.server;

import legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_7_10.net.minecraft.network.Packet;

/** Tile entity description packet; becomes a modern ClientboundBlockEntityDataPacket. */
public class S35PacketUpdateTileEntity extends Packet {
    // 1.7.10's field names; mods read them directly through access transformers (DragonAPI reads nbt)
    public int x;
    public int y;
    public int z;
    public int metadata;
    public NBTTagCompound nbt;

    public S35PacketUpdateTileEntity() {}

    public S35PacketUpdateTileEntity(int x, int y, int z, int metadata, NBTTagCompound tag) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.metadata = metadata;
        this.nbt = tag;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
    }

    public int getTileEntityType() {
        return metadata;
    }

    public NBTTagCompound getNbtCompound() {
        return nbt;
    }
}
