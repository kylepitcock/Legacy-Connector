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

    /**
     * A copy of the section's sky light as the modern light engine computed it (same nibble layout, y << 8 | z << 4 | x):
     * full light where the engine holds none (a section a mod built itself, or one above all terrain). Writes to it change nothing.
     */
    public legacyconnector.v1_7_10.net.minecraft.world.chunk.NibbleArray getSkylightArray() {
        return mcc$light(net.minecraft.world.level.LightLayer.SKY, 15);
    }

    /** A copy of the section's block light (see {@link #getSkylightArray}); dark where the engine holds none. */
    public legacyconnector.v1_7_10.net.minecraft.world.chunk.NibbleArray getBlocklightArray() {
        return mcc$light(net.minecraft.world.level.LightLayer.BLOCK, 0);
    }

    private legacyconnector.v1_7_10.net.minecraft.world.chunk.NibbleArray mcc$light(net.minecraft.world.level.LightLayer layer, int fallback) {
        byte[] data = null;
        if (ownBlocks == null && chunk != null && chunk.worldObj != null && chunk.mcc$modern() != null) {
            try {
                var engine = chunk.worldObj.mcc$level().getLightEngine();
                var section = engine == null ? null : engine.getLayerListener(layer)
                        .getDataLayerData(net.minecraft.core.SectionPos.of(chunk.xPosition, yBase >> 4, chunk.zPosition));
                if (section != null) data = section.getData().clone();
            } catch (RuntimeException e) {
                // no light data: the fallback below
            }
        }
        if (data == null) {
            data = new byte[2048];
            java.util.Arrays.fill(data, (byte) (fallback | fallback << 4));
        }
        return new legacyconnector.v1_7_10.net.minecraft.world.chunk.NibbleArray(data, 4);
    }

    /** The low eight bits of each block's legacy id (index y << 8 | z << 4 | x), as a copy: writes to it change nothing. */
    public byte[] getBlockLSBArray() {
        byte[] out = new byte[4096];
        for (int i = 0; i < 4096; i++) out[i] = (byte) (Block.getIdFromBlock(getBlockByExtId(i & 15, i >> 8, i >> 4 & 15)) & 255);
        return out;
    }

    /** The high four bits of the legacy ids above 255 (null when every id fits in eight bits, as 1.7.10 left it); a copy. */
    public legacyconnector.v1_7_10.net.minecraft.world.chunk.NibbleArray getBlockMSBArray() {
        legacyconnector.v1_7_10.net.minecraft.world.chunk.NibbleArray out = null;
        for (int i = 0; i < 4096; i++) {
            int x = i & 15, y = i >> 8, z = i >> 4 & 15;
            int id = Block.getIdFromBlock(getBlockByExtId(x, y, z));
            if (id <= 255) continue;
            if (out == null) out = new legacyconnector.v1_7_10.net.minecraft.world.chunk.NibbleArray(4096, 4);
            out.set(x, y, z, id >> 8 & 15);
        }
        return out;
    }

    public void setExtBlockID(int x, int y, int z, Block block) {
        func_150818_a(x, y, z, block);
    }
}
