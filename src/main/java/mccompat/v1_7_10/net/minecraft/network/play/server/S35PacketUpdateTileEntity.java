package mccompat.v1_7_10.net.minecraft.network.play.server;

import mccompat.v1_7_10.net.minecraft.nbt.NBTTagCompound;
import mccompat.v1_7_10.net.minecraft.network.Packet;

/** Tile entity description packet; becomes a modern ClientboundBlockEntityDataPacket. */
public class S35PacketUpdateTileEntity extends Packet {
    private final int x;
    private final int y;
    private final int z;
    private final int metadata;
    private final NBTTagCompound tag;

    public S35PacketUpdateTileEntity(int x, int y, int z, int metadata, NBTTagCompound tag) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.metadata = metadata;
        this.tag = tag;
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
        return tag;
    }
}
