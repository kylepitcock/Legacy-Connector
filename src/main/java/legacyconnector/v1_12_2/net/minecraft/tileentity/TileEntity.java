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

    /** Reads the position and Forge's custom data (mods call super first, then read their own keys). */
    public void readFromNBT(NBTTagCompound compound) {
        if (compound.hasKey("x") && compound.hasKey("y") && compound.hasKey("z")) {
            this.pos = new BlockPos(compound.getInteger("x"), compound.getInteger("y"), compound.getInteger("z"));
        }
        if (compound.hasKey("ForgeData", 10)) this.customTileData = compound.getCompoundTag("ForgeData");
    }

    /** Writes the registered id, the position and Forge's custom data, as 1.12 did. */
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        String id = org.kylepitcock.legacyconnector.v1_12_2.bridge.TileEntityBridge1122.nameOf(getClass());
        if (id != null) compound.setString("id", id);
        compound.setInteger("x", pos.getX());
        compound.setInteger("y", pos.getY());
        compound.setInteger("z", pos.getZ());
        if (customTileData != null) compound.setTag("ForgeData", customTileData);
        return compound;
    }

    private NBTTagCompound customTileData;
    /** The block of this tile entity, cached (updateContainingBlockInfo clears it). */
    protected Block blockType;
    protected int blockMetadata = -1;
    /** Render boxes covering everything (beacons, big multiblock renderers). */
    public static final legacyconnector.v1_12_2.net.minecraft.util.math.AxisAlignedBB INFINITE_EXTENT_AABB = new legacyconnector.v1_12_2.net.minecraft.util.math.AxisAlignedBB(
            Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY,
            Double.POSITIVE_INFINITY);

    /** Forge: data other mods may attach to any tile entity, saved under "ForgeData". */
    public NBTTagCompound getTileData() {
        if (customTileData == null) customTileData = new NBTTagCompound();
        return customTileData;
    }

    /** A block event (chest lids, note blocks) arriving at the tile entity; true when it was handled. */
    public boolean receiveClientEvent(int id, int type) {
        return false;
    }

    public void updateContainingBlockInfo() {
        blockType = null;
        blockMetadata = -1;
    }

    public double getDistanceSq(double x, double y, double z) {
        double dx = pos.getX() + 0.5D - x;
        double dy = pos.getY() + 0.5D - y;
        double dz = pos.getZ() + 0.5D - z;
        return dx * dx + dy * dy + dz * dz;
    }

    public double getMaxRenderDistanceSquared() {
        return 4096.0D;
    }

    /** The box the renderer culls with: the block itself. */
    public legacyconnector.v1_12_2.net.minecraft.util.math.AxisAlignedBB getRenderBoundingBox() {
        return new legacyconnector.v1_12_2.net.minecraft.util.math.AxisAlignedBB(pos, pos.add(1, 1, 1));
    }

    public boolean shouldRenderInPass(int pass) {
        return pass == 0;
    }

    public boolean canRenderBreaking() {
        return false;
    }

    public boolean hasFastRenderer() {
        return false;
    }

    public boolean onlyOpsCanSetNbt() {
        return false;
    }

    public void onChunkUnloadForge() {
        onChunkUnload();
    }

    public boolean restrictNBTCopy() {
        return false;
    }

    public NBTTagCompound serializeNBT() {
        return writeToNBT(new NBTTagCompound());
    }

    public void deserializeNBT(NBTTagCompound nbt) {
        readFromNBT(nbt);
    }

    /** The registered name of a tile entity class. */
    public static legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation getKey(Class<? extends TileEntity> cls) {
        String name = org.kylepitcock.legacyconnector.v1_12_2.bridge.TileEntityBridge1122.nameOf(cls);
        return name == null ? null : new legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation(name);
    }

    /** A tile entity made from saved data by its "id" (null when the id is unknown). */
    public static TileEntity create(World world, NBTTagCompound compound) {
        var cls = org.kylepitcock.legacyconnector.v1_12_2.bridge.TileEntityBridge1122.classOf(compound.getString("id"));
        if (cls == null) return null;
        try {
            var ctor = cls.getDeclaredConstructor();
            ctor.setAccessible(true);
            TileEntity te = ctor.newInstance();
            te.setWorld(world);
            te.readFromNBT(compound);
            return te;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            org.slf4j.LoggerFactory.getLogger("LegacyConnector/TileEntities").warn("Could not create the tile entity {}: {}", compound.getString("id"), e.toString());
            return null;
        }
    }

    public NBTTagCompound getUpdateTag() {
        return writeToNBT(new NBTTagCompound());
    }

    public void handleUpdateTag(NBTTagCompound tag) {
        readFromNBT(tag);
    }

    public void markDirty() {
        if (mcc$host != null) mcc$host.setChanged();
    }

    public Block getBlockType() {
        if (blockType == null && mcc$host != null) blockType = org.kylepitcock.legacyconnector.v1_12_2.bridge.StatesBridge1122.legacyBlock(mcc$host.getBlockState());
        return blockType;
    }

    public int getBlockMetadata() {
        if (mcc$host == null) return blockMetadata < 0 ? 0 : blockMetadata;
        var modern = mcc$host.hasLevel() ? mcc$host.getLevel().getBlockState(mcc$host.getBlockPos()) : mcc$host.getBlockState();
        var state = org.kylepitcock.legacyconnector.v1_12_2.bridge.StatesBridge1122.toLegacy(modern);
        var block = state == null ? null : state.getBlock();
        blockMetadata = block == null ? 0 : block.getMetaFromState(state);
        return blockMetadata;
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
