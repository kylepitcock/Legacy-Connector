package legacyconnector.v1_6_4.net.minecraft.network.packet;

import legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_7_10.net.minecraft.network.play.server.S35PacketUpdateTileEntity;

/**
 * 1.6.4's tile entity description packet. It is the 1.7.10 shim's {@link S35PacketUpdateTileEntity} under 1.6.4's field names, so the
 * layer's tile entity sync (a block entity update carrying the tag) handles it; the client end hands it to the tile's 1.6.4
 * {@code onDataPacket(INetworkManager, Packet132TileEntityData)} (see {@code bridge.Tiles164}).
 */
public class Packet132TileEntityData extends S35PacketUpdateTileEntity {
    public int xPosition;
    public int yPosition;
    public int zPosition;
    public int actionType;
    public NBTTagCompound data;

    public Packet132TileEntityData() {}

    public Packet132TileEntityData(int x, int y, int z, int actionType, NBTTagCompound tag) {
        this.xPosition = x;
        this.yPosition = y;
        this.zPosition = z;
        this.actionType = actionType;
        this.data = tag;
    }

    @Override
    public int getX() {
        return xPosition;
    }

    @Override
    public int getY() {
        return yPosition;
    }

    @Override
    public int getZ() {
        return zPosition;
    }

    @Override
    public int getTileEntityType() {
        return actionType;
    }

    @Override
    public NBTTagCompound getNbtCompound() {
        return data;
    }
}
