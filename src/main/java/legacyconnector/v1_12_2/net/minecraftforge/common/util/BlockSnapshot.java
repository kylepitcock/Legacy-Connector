package legacyconnector.v1_12_2.net.minecraftforge.common.util;

import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.StatesBridge1122;

/**
 * A block (state and tile entity data) as it was before a change, which {@link #restore} puts back. Snapshots of the modern game
 * (placement events) wrap the modern snapshot; ones mods make read the block now.
 */
public class BlockSnapshot {
    private final BlockPos pos;
    private final int dimId;
    private IBlockState replacedBlock;
    private int flag;
    private final NBTTagCompound nbt;
    private World world;
    /** The modern snapshot this one stands for, when there is one. */
    public net.neoforged.neoforge.common.util.BlockSnapshot mcc$modern;

    public BlockSnapshot(World world, BlockPos pos, IBlockState state) {
        this(world, pos, state, mcc$tileData(world, pos));
    }

    public BlockSnapshot(World world, BlockPos pos, IBlockState state, NBTTagCompound nbt) {
        this(world, pos, state, nbt, 3);
    }

    public BlockSnapshot(World world, BlockPos pos, IBlockState state, int flag) {
        this(world, pos, state, mcc$tileData(world, pos), flag);
    }

    private BlockSnapshot(World world, BlockPos pos, IBlockState state, NBTTagCompound nbt, int flag) {
        this.world = world;
        this.pos = pos.toImmutable();
        this.replacedBlock = state;
        this.nbt = nbt;
        this.flag = flag;
        var provider = world == null ? null : World.mcc$get$provider(world);
        this.dimId = provider == null ? 0 : provider.getDimension();
    }

    /** The legacy view of a modern snapshot. */
    public static BlockSnapshot mcc$of(net.neoforged.neoforge.common.util.BlockSnapshot modern) {
        if (modern == null) return null;
        World world = modern.getLevel() instanceof net.minecraft.world.level.Level l ? org.kylepitcock.legacyconnector.v1_12_2.bridge.WorldBridge1122.wrap(l) : null;
        var tag = modern.getTag();
        BlockSnapshot out = new BlockSnapshot(world, BlockPos.mcc$of(modern.getPos()), StatesBridge1122.toLegacy(modern.getState()),
                tag == null ? null : new NBTTagCompound(tag), modern.getFlags());
        out.mcc$modern = modern;
        return out;
    }

    private static NBTTagCompound mcc$tileData(World world, BlockPos pos) {
        var level = world == null ? null : world.mcc$level();
        if (level == null) return null;
        var be = level.getBlockEntity(pos.mcc$modern());
        return be == null ? null : new NBTTagCompound(be.saveWithFullMetadata(level.registryAccess()));
    }

    public static BlockSnapshot getBlockSnapshot(World world, BlockPos pos) {
        return new BlockSnapshot(world, pos, world.getBlockState(pos));
    }

    public static BlockSnapshot getBlockSnapshot(World world, BlockPos pos, int flag) {
        return new BlockSnapshot(world, pos, world.getBlockState(pos), flag);
    }

    /** A snapshot saved with {@link #writeToNBT}, in the dimension it names. */
    public static BlockSnapshot readFromNBT(NBTTagCompound tag) {
        NBTTagCompound nbt = tag.getBoolean("hasTE") ? tag.getCompoundTag("tileEntity") : null;
        World world = legacyconnector.v1_12_2.net.minecraftforge.common.DimensionManager.getWorld(tag.getInteger("dimension"));
        var block = legacyconnector.v1_12_2.net.minecraft.block.Block.getBlockFromName(tag.getString("blockMod") + ":" + tag.getString("blockName"));
        IBlockState state = block == null ? null : block.getStateFromMeta(tag.getInteger("metadata"));
        return new BlockSnapshot(world, new BlockPos(tag.getInteger("posX"), tag.getInteger("posY"), tag.getInteger("posZ")), state, nbt,
                tag.getInteger("flag"));
    }

    public void writeToNBT(NBTTagCompound compound) {
        var name = replacedBlock == null || replacedBlock.getBlock() == null ? null : replacedBlock.getBlock().getRegistryName();
        compound.setString("blockMod", name == null ? "minecraft" : name.getNamespace());
        compound.setString("blockName", name == null ? "air" : name.getPath());
        compound.setInteger("posX", pos.getX());
        compound.setInteger("posY", pos.getY());
        compound.setInteger("posZ", pos.getZ());
        compound.setInteger("flag", flag);
        compound.setInteger("dimension", dimId);
        compound.setInteger("metadata", replacedBlock == null ? 0 : replacedBlock.getBlock().getMetaFromState(replacedBlock));
        compound.setBoolean("hasTE", nbt != null);
        if (nbt != null) compound.setTag("tileEntity", nbt);
    }

    public World getWorld() {
        if (world == null) world = legacyconnector.v1_12_2.net.minecraftforge.common.DimensionManager.getWorld(dimId);
        return world;
    }

    public BlockPos getPos() {
        return pos;
    }

    public IBlockState getReplacedBlock() {
        return replacedBlock;
    }

    public void setReplacedBlock(IBlockState state) {
        this.replacedBlock = state;
    }

    public IBlockState getCurrentBlock() {
        return getWorld() == null ? null : getWorld().getBlockState(pos);
    }

    public int getFlag() {
        return flag;
    }

    public void setFlag(int flag) {
        this.flag = flag;
    }

    public NBTTagCompound getNbt() {
        return nbt;
    }

    public int getDimId() {
        return dimId;
    }

    public legacyconnector.v1_12_2.net.minecraft.tileentity.TileEntity getTileEntity() {
        return getWorld() == null ? null : getWorld().getTileEntity(pos);
    }

    public boolean restore() {
        return restore(false);
    }

    public boolean restore(boolean force) {
        return restore(force, true);
    }

    public boolean restore(boolean force, boolean notifyNeighbors) {
        return restoreToLocation(getWorld(), pos, force, notifyNeighbors);
    }

    /**
     * Puts the snapshot's block back (only if the block there is still the one that replaced it, unless forced), then its tile
     * entity data.
     */
    public boolean restoreToLocation(World world, BlockPos pos, boolean force, boolean notifyNeighbors) {
        var level = world == null ? null : world.mcc$level();
        if (level == null || replacedBlock == null) return false;
        var target = pos.mcc$modern();
        int flags = notifyNeighbors ? flag : flag & ~1;
        if (mcc$modern != null && force) return mcc$modern.restoreToLocation(level, target, flags);
        var modernState = StatesBridge1122.toModern(replacedBlock);
        if (modernState == null) return false;
        if (!force && mcc$modern != null && level.getBlockState(target).getBlock() != mcc$modern.getCurrentState().getBlock()) return false;
        level.setBlock(target, modernState, flags);
        if (nbt != null) {
            var be = level.getBlockEntity(target);
            if (be != null) {
                var tag = nbt.mcc$modern().copy();
                tag.putInt("x", target.getX());
                tag.putInt("y", target.getY());
                tag.putInt("z", target.getZ());
                be.loadWithComponents(tag, level.registryAccess());
                be.setChanged();
            }
        }
        return true;
    }
}
