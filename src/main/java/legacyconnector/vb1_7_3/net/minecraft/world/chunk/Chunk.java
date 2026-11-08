package legacyconnector.vb1_7_3.net.minecraft.world.chunk;

import legacyconnector.vb1_7_3.net.minecraft.block.entity.BlockEntity;
import legacyconnector.vb1_7_3.net.minecraft.world.World;

/**
 * Beta 1.7.3 {@code Chunk} as a view: a 16x16 column of the world at chunk {@link #x}, {@link #z}; reads and writes go to the world with
 * chunk-relative coordinates. Mods mostly ask it to be saved ({@link #markDirty}, RedPower after changing a tile entity).
 */
public class Chunk {
    public World world;
    public final int x;
    public final int z;
    public boolean dirty;
    public boolean loaded = true;

    public Chunk(World world, int x, int z) {
        this.world = world;
        this.x = x;
        this.z = z;
    }

    public boolean chunkPosEquals(int x, int z) {
        return this.x == x && this.z == z;
    }

    public int getHeight(int x, int z) {
        return world.getTopY((this.x << 4) + x, (this.z << 4) + z);
    }

    public int getBlockId(int x, int y, int z) {
        return world.getBlockId((this.x << 4) + x, y, (this.z << 4) + z);
    }

    public int getBlockMeta(int x, int y, int z) {
        return world.getBlockMeta((this.x << 4) + x, y, (this.z << 4) + z);
    }

    public boolean setBlock(int x, int y, int z, int id) {
        return world.setBlockWithoutNotifyingNeighbors((this.x << 4) + x, y, (this.z << 4) + z, id);
    }

    public boolean setBlock(int x, int y, int z, int id, int meta) {
        return world.setBlockWithoutNotifyingNeighbors((this.x << 4) + x, y, (this.z << 4) + z, id, meta);
    }

    public void setBlockMeta(int x, int y, int z, int meta) {
        world.setBlockMetaWithoutNotifyingNeighbors((this.x << 4) + x, y, (this.z << 4) + z, meta);
    }

    public BlockEntity getBlockEntity(int x, int y, int z) {
        return world.getBlockEntity((this.x << 4) + x, y, (this.z << 4) + z);
    }

    public void setBlockEntity(int x, int y, int z, BlockEntity te) {
        world.setBlockEntity((this.x << 4) + x, y, (this.z << 4) + z, te);
    }

    public void removeBlockEntityAt(int x, int y, int z) {
        world.removeBlockEntity((this.x << 4) + x, y, (this.z << 4) + z);
    }

    /** MCP {@code setChunkModified}: the modern chunk is saved. */
    public void markDirty() {
        dirty = true;
        net.minecraft.world.level.Level level = world == null ? null : world.mcc$level();
        if (level != null && level.hasChunk(x, z)) level.getChunk(x, z).setUnsaved(true);
    }

    public boolean isEmpty() {
        return false;
    }

    public void load() {}

    public void unload() {}
}
