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
    /** A section a mod created itself ({@code new ExtendedBlockStorage(y, true)}) holds its own data until it is merged into the chunk. */
    private Block[] ownBlocks;
    private byte[] ownMeta;
    private boolean ownDirty;

    public ExtendedBlockStorage(int yBase, boolean hasSky) {
        this(null, yBase);
        this.ownBlocks = new Block[4096];
        this.ownMeta = new byte[4096];
    }

    public boolean mcc$standalone() {
        return ownBlocks != null;
    }

    /** Copies what was written into this section's own storage into the detached chunk's arrays (once per change). */
    public void mcc$mergeInto(Block[] blocks, byte[] meta) {
        if (ownBlocks == null || !ownDirty) return;
        ownDirty = false;
        for (int y = 0; y < 16; y++) {
            for (int z = 0; z < 16; z++) {
                for (int x = 0; x < 16; x++) {
                    int i = y << 8 | z << 4 | x;
                    Block b = ownBlocks[i];
                    if (b == null || b == Blocks.air) continue;
                    int at = (x * 16 + z) * (blocks.length / 256) + yBase + y;
                    blocks[at] = b;
                    meta[at] = ownMeta[i];
                }
            }
        }
    }

    public ExtendedBlockStorage(Chunk chunk, int yBase) {
        this.chunk = chunk;
        this.yBase = yBase;
    }

    public int getYLocation() {
        return yBase;
    }

    public Block getBlockByExtId(int x, int y, int z) {
        if (ownBlocks != null) {
            Block b = ownBlocks[(y & 15) << 8 | (z & 15) << 4 | (x & 15)];
            return b == null ? Blocks.air : b;
        }
        return chunk == null ? Blocks.air : chunk.getBlock(x, yBase + y, z);
    }

    public void func_150818_a(int x, int y, int z, Block block) {
        if (ownBlocks != null) {
            ownBlocks[(y & 15) << 8 | (z & 15) << 4 | (x & 15)] = block;
            ownDirty = true;
        } else if (chunk != null) {
            chunk.func_150807_a(x, yBase + y, z, block, 0);
        }
    }

    public int getExtBlockMetadata(int x, int y, int z) {
        if (ownBlocks != null) return ownMeta[(y & 15) << 8 | (z & 15) << 4 | (x & 15)] & 15;
        return chunk == null ? 0 : chunk.getBlockMetadata(x, yBase + y, z);
    }

    public void setExtBlockMetadata(int x, int y, int z, int meta) {
        if (ownBlocks != null) {
            ownMeta[(y & 15) << 8 | (z & 15) << 4 | (x & 15)] = (byte) meta;
            ownDirty = true;
        } else if (chunk != null) {
            chunk.setBlockMetadata(x, yBase + y, z, meta);
        }
    }

    public boolean isEmpty() {
        if (ownBlocks != null) {
            for (Block b : ownBlocks) if (b != null && b != Blocks.air) return false;
            return true;
        }
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

    public void setExtBlockID(int x, int y, int z, Block block) {
        func_150818_a(x, y, z, block);
    }
}
