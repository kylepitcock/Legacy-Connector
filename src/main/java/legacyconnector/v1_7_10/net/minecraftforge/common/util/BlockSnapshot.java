package legacyconnector.v1_7_10.net.minecraftforge.common.util;

import legacyconnector.v1_7_10.net.minecraft.block.Block;
import legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/** A block, its metadata and its tile entity's data at a position, taken before a change so the change can be undone. */
public class BlockSnapshot {
    public final int x;
    public final int y;
    public final int z;
    public final int dimId;
    public final World world;
    public final Block replacedBlock;
    public final int meta;
    public int flag = 3;
    /** The tile entity's saved data, or null when there was none. */
    public final NBTTagCompound nbt;

    public BlockSnapshot(World world, int x, int y, int z, Block block, int meta) {
        this(world, x, y, z, block, meta, mcc$tileData(world, x, y, z));
    }

    public BlockSnapshot(World world, int x, int y, int z, Block block, int meta, NBTTagCompound nbt) {
        this.world = world;
        var provider = world == null ? null : World.mcc$get$provider(world);
        this.dimId = provider == null ? 0 : provider.dimensionId;
        this.x = x;
        this.y = y;
        this.z = z;
        this.replacedBlock = block;
        this.meta = meta;
        this.nbt = nbt;
    }

    public BlockSnapshot(World world, int x, int y, int z, Block block, int meta, int flag) {
        this(world, x, y, z, block, meta);
        this.flag = flag;
    }

    private static NBTTagCompound mcc$tileData(World world, int x, int y, int z) {
        if (world == null) return null;
        try {
            var te = world.getTileEntity(x, y, z);
            if (te == null) return null;
            NBTTagCompound tag = new NBTTagCompound();
            te.writeToNBT(tag);
            return tag;
        } catch (RuntimeException | LinkageError e) {
            return null;
        }
    }

    public static BlockSnapshot getBlockSnapshot(World world, int x, int y, int z) {
        return new BlockSnapshot(world, x, y, z, world.getBlock(x, y, z), world.getBlockMetadata(x, y, z));
    }

    public static BlockSnapshot getBlockSnapshot(World world, int x, int y, int z, int flag) {
        return new BlockSnapshot(world, x, y, z, world.getBlock(x, y, z), world.getBlockMetadata(x, y, z), flag);
    }

    /** The block at the snapshot's position now. */
    public Block getCurrentBlock() {
        return world.getBlock(x, y, z);
    }

    public World getWorld() {
        return world;
    }

    public boolean restore() {
        return restore(false);
    }

    public boolean restore(boolean force) {
        return restore(force, true);
    }

    /**
     * Puts the snapshot back: the block (only when {@code force} if another block is there now, else false), its
     * metadata and its tile entity's data; {@code applyPhysics} notifies the neighbours.
     */
    public boolean restore(boolean force, boolean applyPhysics) {
        int flags = applyPhysics ? 3 : 2;
        if (getCurrentBlock() != replacedBlock || world.getBlockMetadata(x, y, z) != meta) {
            if (!force) return false;
            world.setBlock(x, y, z, replacedBlock, meta, flags);
        }
        world.setBlockMetadataWithNotify(x, y, z, meta, flags);
        world.markBlockForUpdate(x, y, z);
        if (nbt != null) {
            var te = world.getTileEntity(x, y, z);
            if (te != null) te.readFromNBT(nbt);
        }
        return true;
    }
}
