package legacyconnector.v1_7_10.net.minecraft.world.chunk.storage;

import legacyconnector.v1_7_10.net.minecraft.block.Block;
import legacyconnector.v1_7_10.net.minecraft.init.Blocks;
import legacyconnector.v1_7_10.net.minecraft.world.chunk.Chunk;

/**
 * A 16-block-high section of a chunk (legacy direct chunk storage). A view: reads and writes go through the chunk's
 * world, so mods that write ores or blocks straight into storage change the real modern chunk.
 */
public class ExtendedBlockStorage {
    private final int yBase;
    private final Chunk chunk;

    public ExtendedBlockStorage(int yBase, boolean hasSky) {
        this(null, yBase);
    }

    public ExtendedBlockStorage(Chunk chunk, int yBase) {
        this.chunk = chunk;
        this.yBase = yBase;
    }

    public int getYLocation() {
        return yBase;
    }

    public Block getBlockByExtId(int x, int y, int z) {
        return chunk == null ? Blocks.air : chunk.getBlock(x, yBase + y, z);
    }

    public void func_150818_a(int x, int y, int z, Block block) {
        if (chunk != null) chunk.func_150807_a(x, yBase + y, z, block, 0);
    }

    public int getExtBlockMetadata(int x, int y, int z) {
        return chunk == null ? 0 : chunk.getBlockMetadata(x, yBase + y, z);
    }

    public void setExtBlockMetadata(int x, int y, int z, int meta) {
        if (chunk != null) chunk.setBlockMetadata(x, yBase + y, z, meta);
    }

    public boolean isEmpty() {
        return chunk == null || chunk.getAreLevelsEmpty(yBase, yBase + 15);
    }

    public boolean getNeedsRandomTick() {
        return false;
    }

    public int getExtSkylightValue(int x, int y, int z) {
        return 15;
    }

    public void setExtSkylightValue(int x, int y, int z, int value) {
    }

    public int getExtBlocklightValue(int x, int y, int z) {
        return 0;
    }

    public void setExtBlocklightValue(int x, int y, int z, int value) {
    }

    public void removeInvalidBlocks() {
    }
}
