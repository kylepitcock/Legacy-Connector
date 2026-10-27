package legacyconnector.v1_12_2.net.minecraft.world.chunk.storage;

import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.world.chunk.Chunk;
import legacyconnector.v1_12_2.net.minecraft.world.chunk.NibbleArray;

/**
 * 1.12.2 16-block-high chunk section. A section of a chunk ({@code Chunk.getBlockStorageArray}) reads and writes through that
 * chunk (the modern chunk, or a generator's primer); one a mod creates keeps its own states and light.
 */
public class ExtendedBlockStorage {
    private final int yBase;
    private final Chunk mcc$chunk;
    private final IBlockState[] mcc$states;
    private NibbleArray blockLight = new NibbleArray();
    private NibbleArray skyLight;
    private int blockRefCount;

    public ExtendedBlockStorage(int y, boolean storeSkylight) {
        this.yBase = y;
        this.mcc$chunk = null;
        this.mcc$states = new IBlockState[4096];
        if (storeSkylight) this.skyLight = new NibbleArray();
    }

    /** A view of the section of a chunk starting at {@code y}. */
    public ExtendedBlockStorage(Chunk chunk, int y) {
        this.yBase = y;
        this.mcc$chunk = chunk;
        this.mcc$states = null;
        this.skyLight = new NibbleArray();
    }

    private static IBlockState air() {
        return org.kylepitcock.legacyconnector.v1_12_2.bridge.Vanilla1122.airState();
    }

    public IBlockState get(int x, int y, int z) {
        if (mcc$chunk != null) return mcc$chunk.getBlockState(x & 15, yBase + (y & 15), z & 15);
        IBlockState s = mcc$states[y << 8 | z << 4 | x];
        return s == null ? air() : s;
    }

    public void set(int x, int y, int z, IBlockState state) {
        if (mcc$chunk != null) {
            mcc$chunk.setBlockState(new legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos((mcc$chunk.x << 4) + (x & 15), yBase + (y & 15),
                    (mcc$chunk.z << 4) + (z & 15)), state);
            return;
        }
        int i = y << 8 | z << 4 | x;
        IBlockState old = mcc$states[i];
        boolean wasAir = old == null || old.getMaterial() == legacyconnector.v1_12_2.net.minecraft.block.material.Material.AIR;
        boolean isAir = state == null || state.getMaterial() == legacyconnector.v1_12_2.net.minecraft.block.material.Material.AIR;
        if (!wasAir) blockRefCount--;
        if (!isAir) blockRefCount++;
        mcc$states[i] = state;
    }

    public boolean isEmpty() {
        if (mcc$chunk != null) return mcc$chunk.isEmptyBetween(yBase, yBase + 15);
        return blockRefCount == 0;
    }

    public boolean needsRandomTick() {
        return !isEmpty();
    }

    public int getYLocation() {
        return yBase;
    }

    public void setSkyLight(int x, int y, int z, int value) {
        if (skyLight != null) skyLight.set(x, y, z, value);
    }

    public int getSkyLight(int x, int y, int z) {
        if (mcc$chunk != null && mcc$chunk.getWorld() != null) {
            return mcc$chunk.getLightFor(legacyconnector.v1_12_2.net.minecraft.world.EnumSkyBlock.SKY,
                    new legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos((mcc$chunk.x << 4) + x, yBase + y, (mcc$chunk.z << 4) + z));
        }
        return skyLight == null ? 0 : skyLight.get(x, y, z);
    }

    public void setBlockLight(int x, int y, int z, int value) {
        blockLight.set(x, y, z, value);
    }

    public int getBlockLight(int x, int y, int z) {
        if (mcc$chunk != null && mcc$chunk.getWorld() != null) {
            return mcc$chunk.getLightFor(legacyconnector.v1_12_2.net.minecraft.world.EnumSkyBlock.BLOCK,
                    new legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos((mcc$chunk.x << 4) + x, yBase + y, (mcc$chunk.z << 4) + z));
        }
        return blockLight.get(x, y, z);
    }

    public void recalculateRefCounts() {
        if (mcc$states == null) return;
        blockRefCount = 0;
        for (IBlockState s : mcc$states) {
            if (s != null && s.getMaterial() != legacyconnector.v1_12_2.net.minecraft.block.material.Material.AIR) blockRefCount++;
        }
    }

    public NibbleArray getBlockLight() {
        return blockLight;
    }

    public NibbleArray getSkyLight() {
        return skyLight;
    }

    public void setBlockLight(NibbleArray array) {
        this.blockLight = array;
    }

    public void setSkyLight(NibbleArray array) {
        this.skyLight = array;
    }
}
