package legacyconnector.v1_7_10.net.minecraftforge.common.util;

import legacyconnector.v1_7_10.net.minecraft.block.Block;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/** A block and its metadata at a position, taken before a change so the change can be undone. */
public class BlockSnapshot {
    public final int x;
    public final int y;
    public final int z;
    public final World world;
    public final Block replacedBlock;
    public final int meta;
    public int flag = 3;

    public BlockSnapshot(World world, int x, int y, int z, Block block, int meta) {
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.replacedBlock = block;
        this.meta = meta;
    }

    public static BlockSnapshot getBlockSnapshot(World world, int x, int y, int z) {
        return new BlockSnapshot(world, x, y, z, world.getBlock(x, y, z), world.getBlockMetadata(x, y, z));
    }

    /** The block at the snapshot's position now. */
    public Block getCurrentBlock() {
        return world.getBlock(x, y, z);
    }

    public boolean restore() {
        return world.setBlock(x, y, z, replacedBlock, meta, flag);
    }
}
