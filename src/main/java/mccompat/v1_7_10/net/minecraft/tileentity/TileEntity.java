package mccompat.v1_7_10.net.minecraft.tileentity;

import mccompat.v1_7_10.net.minecraft.block.Block;
import mccompat.v1_7_10.net.minecraft.nbt.NBTTagCompound;
import mccompat.v1_7_10.net.minecraft.network.NetworkManager;
import mccompat.v1_7_10.net.minecraft.network.Packet;
import mccompat.v1_7_10.net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import mccompat.v1_7_10.net.minecraft.util.AxisAlignedBB;
import mccompat.v1_7_10.net.minecraft.world.World;
import org.kylepitcock.mccompat.v1_7_10.bridge.TileEntityBridge;

/**
 * 1.7.10 tile entity. Legacy tile entities are logic objects hosted by a modern {@code LegacyBlockEntity},
 * which keeps {@link #worldObj} and the coordinates up to date, ticks {@link #updateEntity()} when
 * {@link #canUpdate()} is true, and serializes through {@link #writeToNBT}/{@link #readFromNBT} using the
 * legacy NBT layout (including the legacy {@code "id"} string).
 */
public class TileEntity {
    public World worldObj;
    public int xCoord;
    public int yCoord;
    public int zCoord;
    protected boolean tileEntityInvalid;
    public int blockMetadata = -1;
    public Block blockType;

    /** The modern host; set by the bridge. */
    public net.minecraft.world.level.block.entity.BlockEntity mcc$host;

    public static void addMapping(Class<?> cls, String id) {
        TileEntityBridge.addMapping(cls, id);
    }

    public static TileEntity createAndLoadEntity(NBTTagCompound tag) {
        return TileEntityBridge.createAndLoad(tag);
    }

    public void readFromNBT(NBTTagCompound tag) {
        this.xCoord = tag.getInteger("x");
        this.yCoord = tag.getInteger("y");
        this.zCoord = tag.getInteger("z");
    }

    public void writeToNBT(NBTTagCompound tag) {
        String id = TileEntityBridge.idOf(getClass());
        if (id == null) throw new RuntimeException(getClass() + " is missing a mapping! This is a bug!");
        tag.setString("id", id);
        tag.setInteger("x", xCoord);
        tag.setInteger("y", yCoord);
        tag.setInteger("z", zCoord);
    }

    public void updateEntity() {
    }

    /** Forge: tile entities that return false are never ticked. */
    public boolean canUpdate() {
        return true;
    }

    public World getWorldObj() {
        return worldObj;
    }

    public void setWorldObj(World world) {
        this.worldObj = world;
    }

    public boolean hasWorldObj() {
        return worldObj != null;
    }

    public int getBlockMetadata() {
        if (blockMetadata == -1 && worldObj != null) blockMetadata = worldObj.getBlockMetadata(xCoord, yCoord, zCoord);
        return blockMetadata;
    }

    public Block getBlockType() {
        if (blockType == null && worldObj != null) blockType = worldObj.getBlock(xCoord, yCoord, zCoord);
        return blockType;
    }

    public void markDirty() {
        if (worldObj != null) {
            blockMetadata = worldObj.getBlockMetadata(xCoord, yCoord, zCoord);
            if (mcc$host != null) mcc$host.setChanged();
        }
    }

    public double getDistanceFrom(double x, double y, double z) {
        double dx = xCoord + 0.5D - x, dy = yCoord + 0.5D - y, dz = zCoord + 0.5D - z;
        return dx * dx + dy * dy + dz * dz;
    }

    public double getMaxRenderDistanceSquared() {
        return 4096.0D;
    }

    public Packet getDescriptionPacket() {
        return null;
    }

    /** Forge: client receives the packet produced by {@link #getDescriptionPacket()}. */
    public void onDataPacket(NetworkManager net, S35PacketUpdateTileEntity pkt) {
    }

    public boolean isInvalid() {
        return tileEntityInvalid;
    }

    public void invalidate() {
        tileEntityInvalid = true;
    }

    public void validate() {
        tileEntityInvalid = false;
    }

    public boolean receiveClientEvent(int id, int type) {
        return false;
    }

    public void updateContainingBlockInfo() {
        blockType = null;
        blockMetadata = -1;
    }

    public void onChunkUnload() {
    }

    public boolean shouldRefresh(Block oldBlock, Block newBlock, int oldMeta, int newMeta, World world, int x, int y, int z) {
        return oldBlock != newBlock;
    }

    public boolean shouldRenderInPass(int pass) {
        return pass == 0;
    }

    public AxisAlignedBB getRenderBoundingBox() {
        return AxisAlignedBB.getBoundingBox(xCoord, yCoord, zCoord, xCoord + 1, yCoord + 1, zCoord + 1);
    }

    public boolean canUpdateWhileInvalid() {
        return false;
    }
}
