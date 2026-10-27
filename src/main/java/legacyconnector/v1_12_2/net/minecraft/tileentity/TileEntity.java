package legacyconnector.v1_12_2.net.minecraft.tileentity;

import legacyconnector.v1_12_2.net.minecraft.block.Block;
import legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;
import legacyconnector.v1_12_2.net.minecraftforge.common.capabilities.Capability;
import legacyconnector.v1_12_2.net.minecraftforge.common.capabilities.ICapabilityProvider;

/** A 1.12.2 tile entity, hosted by a modern block entity once it is placed. */
public abstract class TileEntity implements ICapabilityProvider {
    protected World world;
    protected BlockPos pos = BlockPos.ORIGIN;
    protected boolean tileEntityInvalid;
    /** The modern block entity hosting this tile entity. */
    public net.minecraft.world.level.block.entity.BlockEntity mcc$host;

    public TileEntity() {
    }

    public World getWorld() {
        return world;
    }

    public void setWorld(World world) {
        this.world = world;
    }

    public boolean hasWorld() {
        return world != null;
    }

    public BlockPos getPos() {
        return pos;
    }

    public void setPos(BlockPos pos) {
        this.pos = pos.toImmutable();
    }

    public void readFromNBT(NBTTagCompound compound) {
    }

    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        return compound;
    }

    public NBTTagCompound getUpdateTag() {
        return writeToNBT(new NBTTagCompound());
    }

    public void handleUpdateTag(NBTTagCompound tag) {
        readFromNBT(tag);
    }

    /** The packet that tells clients about this tile entity when it changes; none by default (the update tag is sent). */
    public legacyconnector.v1_12_2.net.minecraft.network.play.server.SPacketUpdateTileEntity getUpdatePacket() {
        return null;
    }

    /** Receives the packet {@link #getUpdatePacket} made, on the client. */
    public void onDataPacket(legacyconnector.v1_12_2.net.minecraft.network.NetworkManager net,
            legacyconnector.v1_12_2.net.minecraft.network.play.server.SPacketUpdateTileEntity pkt) {
    }

    public void markDirty() {
        if (mcc$host != null) mcc$host.setChanged();
    }

    public Block getBlockType() {
        return mcc$host == null ? null : org.kylepitcock.legacyconnector.v1_12_2.bridge.StatesBridge1122.legacyBlock(mcc$host.getBlockState());
    }

    public int getBlockMetadata() {
        var state = org.kylepitcock.legacyconnector.v1_12_2.bridge.StatesBridge1122.toLegacy(mcc$host.getBlockState());
        var block = state == null ? null : state.getBlock();
        return block == null ? 0 : block.getMetaFromState(state);
    }

    public boolean isInvalid() {
        return tileEntityInvalid;
    }

    public void invalidate() {
        this.tileEntityInvalid = true;
    }

    public void validate() {
        this.tileEntityInvalid = false;
    }

    public void onLoad() {
    }

    public void onChunkUnload() {
    }

    public boolean shouldRefresh(World world, BlockPos pos, legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState oldState,
            legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState newState) {
        return oldState.getBlock() != newState.getBlock();
    }

    @Override
    public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
        return false;
    }

    @Override
    public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
        return null;
    }
}
