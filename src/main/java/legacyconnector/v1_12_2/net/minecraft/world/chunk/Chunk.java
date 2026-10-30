package legacyconnector.v1_12_2.net.minecraft.world.chunk;

import java.util.List;
import java.util.Map;
import java.util.Random;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.tileentity.TileEntity;
import legacyconnector.v1_12_2.net.minecraft.util.math.AxisAlignedBB;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.util.math.ChunkPos;
import legacyconnector.v1_12_2.net.minecraft.world.EnumSkyBlock;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/**
 * A 1.12.2 chunk. A chunk of a modern level is a view over its {@code ChunkAccess} (block coordinates are absolute, so heights
 * keep the modern range); a chunk a mod's generator builds from a {@link ChunkPrimer} keeps that primer.
 */
public class Chunk implements legacyconnector.v1_12_2.net.minecraftforge.common.capabilities.ICapabilityProvider {
    public final int x;
    public final int z;
    private final World world;
    private final net.minecraft.world.level.chunk.ChunkAccess access;
    private final ChunkPrimer primer;
    private byte[] blockBiomeArray;
    private boolean terrainPopulated;
    private boolean loaded;
    public boolean unloadQueued;

    public Chunk(World world, int x, int z) {
        this(world, new ChunkPrimer(), x, z);
    }

    public Chunk(World world, ChunkPrimer primer, int x, int z) {
        this.world = world;
        this.x = x;
        this.z = z;
        this.access = null;
        this.primer = primer;
    }

    private Chunk(World world, net.minecraft.world.level.chunk.ChunkAccess access) {
        this.world = world;
        this.access = access;
        this.x = access.getPos().x;
        this.z = access.getPos().z;
        this.primer = null;
        this.loaded = access instanceof net.minecraft.world.level.chunk.LevelChunk;
    }

    /** A view of a modern chunk of the world. */
    public static Chunk mcc$of(World world, net.minecraft.world.level.chunk.ChunkAccess access) {
        return access == null ? null : new Chunk(world, access);
    }

    /** The modern chunk this one views, or null for a chunk built from a primer. */
    public net.minecraft.world.level.chunk.ChunkAccess mcc$access() {
        return access;
    }

    /** The blocks a mod's generator put into this chunk, or null for a view of a modern chunk. */
    public ChunkPrimer mcc$primer() {
        return primer;
    }

    private net.minecraft.core.BlockPos absolute(int bx, int by, int bz) {
        return new net.minecraft.core.BlockPos((x << 4) + (bx & 15), by, (z << 4) + (bz & 15));
    }

    public boolean isAtLocation(int cx, int cz) {
        return cx == x && cz == z;
    }

    public ChunkPos getPos() {
        return new ChunkPos(x, z);
    }

    public World getWorld() {
        return world;
    }

    public int getHeight(BlockPos pos) {
        return getHeightValue(pos.getX() & 15, pos.getZ() & 15);
    }

    public int getHeightValue(int bx, int bz) {
        if (access == null) return primer.findGroundBlockIdx(bx & 15, bz & 15) + 1;
        return access.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, bx & 15, bz & 15) + 1;
    }

    public int getLowestHeight() {
        int low = Integer.MAX_VALUE;
        for (int i = 0; i < 16; i++) for (int j = 0; j < 16; j++) low = Math.min(low, getHeightValue(i, j));
        return low;
    }

    /** The y of the bottom of the highest section holding blocks (-1 for an empty chunk, as 1.12 answered 0 for its lowest). */
    public int getTopFilledSegment() {
        if (access == null) {
            for (int sy = 15; sy >= 0; sy--) for (int i = 0; i < 16; i++) for (int j = 0; j < 16; j++) for (int y = 15; y >= 0; y--) {
                if (primer.mcc$isSet(i, (sy << 4) + y, j)) return sy << 4;
            }
            return 0;
        }
        int idx = access.getHighestFilledSectionIndex();
        return idx < 0 ? 0 : access.getSectionYFromSectionIndex(idx) << 4;
    }

    public int[] getHeightMap() {
        int[] out = new int[256];
        for (int i = 0; i < 16; i++) for (int j = 0; j < 16; j++) out[j << 4 | i] = getHeightValue(i, j);
        return out;
    }

    public void setHeightMap(int[] map) {
    }

    public void generateHeightMap() {
    }

    public void generateSkylightMap() {
    }

    public void resetRelightChecks() {
    }

    public void enqueueRelightChecks() {
    }

    public void checkLight() {
    }

    public int getBlockLightOpacity(BlockPos pos) {
        if (access == null) return 0;
        var p = absolute(pos.getX(), pos.getY(), pos.getZ());
        return access.getBlockState(p).getLightBlock(access, p);
    }

    public IBlockState getBlockState(BlockPos pos) {
        return getBlockState(pos.getX(), pos.getY(), pos.getZ());
    }

    public IBlockState getBlockState(int bx, int by, int bz) {
        if (access == null) return primer.getBlockState(bx & 15, by, bz & 15);
        IBlockState s = org.kylepitcock.legacyconnector.v1_12_2.bridge.StatesBridge1122.toLegacy(access.getBlockState(absolute(bx, by, bz)));
        return s == null ? org.kylepitcock.legacyconnector.v1_12_2.bridge.Vanilla1122.airState() : s;
    }

    /** Sets a block in the chunk without neighbour updates; returns the previous state, or null when nothing changed. */
    public IBlockState setBlockState(BlockPos pos, IBlockState state) {
        IBlockState old = getBlockState(pos);
        if (access == null) {
            primer.setBlockState(pos.getX() & 15, pos.getY(), pos.getZ() & 15, state);
            return old;
        }
        var previous = access.setBlockState(absolute(pos.getX(), pos.getY(), pos.getZ()),
                org.kylepitcock.legacyconnector.v1_12_2.bridge.StatesBridge1122.toModern(state), false);
        return previous == null ? null : old;
    }

    public int getLightFor(EnumSkyBlock type, BlockPos pos) {
        if (world == null || world.mcc$level() == null) return type.defaultLightValue;
        return world.mcc$level().getBrightness(type.mcc$modern(), absolute(pos.getX(), pos.getY(), pos.getZ()));
    }

    public void setLightFor(EnumSkyBlock type, BlockPos pos, int value) {
    }

    public int getLightSubtracted(BlockPos pos, int amount) {
        if (world == null || world.mcc$level() == null) return 0;
        var p = absolute(pos.getX(), pos.getY(), pos.getZ());
        return world.mcc$level().getRawBrightness(p, amount);
    }

    public void addEntity(Entity entity) {
    }

    public void removeEntity(Entity entity) {
    }

    public void removeEntityAtIndex(Entity entity, int index) {
    }

    public boolean canSeeSky(BlockPos pos) {
        return pos.getY() >= getHeightValue(pos.getX() & 15, pos.getZ() & 15);
    }

    public TileEntity getTileEntity(BlockPos pos, EnumCreateEntityType type) {
        if (access == null) return null;
        var be = access.getBlockEntity(absolute(pos.getX(), pos.getY(), pos.getZ()));
        return be instanceof org.kylepitcock.legacyconnector.v1_12_2.adapter.Legacy1122BlockEntity l ? l.legacy() : null;
    }

    public void addTileEntity(TileEntity te) {
        if (te != null && te.getPos() != null) addTileEntity(te.getPos(), te);
    }

    public void addTileEntity(BlockPos pos, TileEntity te) {
        if (world != null && te != null) world.setTileEntity(pos, te);
    }

    public void removeTileEntity(BlockPos pos) {
        if (access != null) access.removeBlockEntity(absolute(pos.getX(), pos.getY(), pos.getZ()));
    }

    /** The legacy tile entities of the chunk by position (a snapshot). */
    public Map<BlockPos, TileEntity> getTileEntityMap() {
        Map<BlockPos, TileEntity> out = new java.util.LinkedHashMap<>();
        if (access == null) return out;
        for (var p : access.getBlockEntitiesPos()) {
            var be = access.getBlockEntity(p);
            if (be instanceof org.kylepitcock.legacyconnector.v1_12_2.adapter.Legacy1122BlockEntity l && l.legacy() != null) {
                out.put(BlockPos.mcc$of(p), l.legacy());
            }
        }
        return out;
    }

    public void onLoad() {
        loaded = true;
    }

    public void onUnload() {
        loaded = false;
    }

    public void markDirty() {
        if (access != null) access.setUnsaved(true);
    }

    public void setModified(boolean modified) {
        if (modified) markDirty();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public void getEntitiesWithinAABBForEntity(Entity except, AxisAlignedBB box, List list, com.google.common.base.Predicate filter) {
        if (world == null) return;
        for (Object o : world.getEntitiesWithinAABBExcludingEntity(except, box)) {
            if (filter == null || filter.apply(o)) list.add(o);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public void getEntitiesOfTypeWithinAABB(Class type, AxisAlignedBB box, List list, com.google.common.base.Predicate filter) {
        if (world == null) return;
        for (Object o : world.getEntitiesWithinAABB(type, box)) {
            if (filter == null || filter.apply(o)) list.add(o);
        }
    }

    public boolean needsSaving(boolean all) {
        return access != null && access.isUnsaved();
    }

    public Random getRandomWithSeed(long seed) {
        long worldSeed = world == null ? 0L : world.getSeed();
        return new Random(worldSeed + (long) (x * x * 4987142) + (long) (x * 5947611) + (long) (z * z) * 4392871L + (long) (z * 389711) ^ seed);
    }

    public boolean isEmpty() {
        return false;
    }

    public void populate(IChunkProvider provider, legacyconnector.v1_12_2.net.minecraft.world.gen.IChunkGenerator generator) {
    }

    public void populate(legacyconnector.v1_12_2.net.minecraft.world.gen.IChunkGenerator generator) {
    }

    public BlockPos getPrecipitationHeight(BlockPos pos) {
        return new BlockPos(pos.getX(), getHeightValue(pos.getX() & 15, pos.getZ() & 15), pos.getZ());
    }

    public void onTick(boolean skipRecheckGaps) {
    }

    public boolean isPopulated() {
        return access == null ? terrainPopulated : access.getPersistedStatus().isOrAfter(net.minecraft.world.level.chunk.status.ChunkStatus.FEATURES);
    }

    public boolean wasTicked() {
        return loaded;
    }

    public boolean isEmptyBetween(int startY, int endY) {
        if (access == null) return true;
        for (int y = startY; y < endY; y += 16) {
            int index = access.getSectionIndex(y);
            if (index >= 0 && index < access.getSections().length && !access.getSections()[index].hasOnlyAir()) return false;
        }
        return true;
    }

    /** 1.12's per-column biome ids (z << 4 | x), from the biomes at the surface. */
    public byte[] getBiomeArray() {
        if (blockBiomeArray != null) return blockBiomeArray;
        byte[] out = new byte[256];
        java.util.Arrays.fill(out, (byte) -1);
        if (access != null) {
            for (int i = 0; i < 16; i++) {
                for (int j = 0; j < 16; j++) {
                    int y = Math.max(access.getMinBuildHeight(), Math.min(access.getMaxBuildHeight() - 1, getHeightValue(i, j)));
                    var holder = access.getNoiseBiome(((x << 4) + i) >> 2, y >> 2, ((z << 4) + j) >> 2);
                    var legacy = org.kylepitcock.legacyconnector.v1_12_2.bridge.BiomeBridge1122.legacy(holder);
                    out[j << 4 | i] = (byte) (legacy == null ? -1 : legacyconnector.v1_12_2.net.minecraft.world.biome.Biome.getIdForBiome(legacy));
                }
            }
        }
        blockBiomeArray = out;
        return out;
    }

    public void setBiomeArray(byte[] biomes) {
        if (biomes != null && biomes.length == 256) blockBiomeArray = biomes;
    }

    public boolean isLoaded() {
        return loaded;
    }

    public void markLoaded(boolean value) {
        loaded = value;
    }

    public boolean isTerrainPopulated() {
        return isPopulated();
    }

    public void setTerrainPopulated(boolean value) {
        terrainPopulated = value;
    }

    public boolean isLightPopulated() {
        return access == null || access.isLightCorrect();
    }

    public void setLightPopulated(boolean value) {
    }

    public void setHasEntities(boolean value) {
    }

    public void setLastSaveTime(long time) {
    }

    public long getInhabitedTime() {
        return access == null ? 0L : access.getInhabitedTime();
    }

    public void setInhabitedTime(long time) {
        if (access != null) access.setInhabitedTime(time);
    }

    @Override
    public boolean hasCapability(legacyconnector.v1_12_2.net.minecraftforge.common.capabilities.Capability<?> capability,
            legacyconnector.v1_12_2.net.minecraft.util.EnumFacing facing) {
        return false;
    }

    @Override
    public <T> T getCapability(legacyconnector.v1_12_2.net.minecraftforge.common.capabilities.Capability<T> capability,
            legacyconnector.v1_12_2.net.minecraft.util.EnumFacing facing) {
        return null;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Chunk c && c.x == x && c.z == z && c.world == world && (c.access == access || access != null && c.access != null);
    }

    @Override
    public int hashCode() {
        return 31 * x + z;
    }

    /** How {@code getTileEntity} treats a missing tile entity. */
    public enum EnumCreateEntityType {
        IMMEDIATE,
        QUEUED,
        CHECK
    }

    // ------------------------------------------------------------------ area W: sections, biome lookup

    /** 1.12's marker for a section that holds nothing. */
    public static final legacyconnector.v1_12_2.net.minecraft.world.chunk.storage.ExtendedBlockStorage NULL_BLOCK_STORAGE = null;
    private legacyconnector.v1_12_2.net.minecraft.world.chunk.storage.ExtendedBlockStorage[] mcc$sections;

    /** The 16 sections of legacy y 0..255 (views that read and write this chunk); an empty one is {@link #NULL_BLOCK_STORAGE}. */
    public legacyconnector.v1_12_2.net.minecraft.world.chunk.storage.ExtendedBlockStorage[] getBlockStorageArray() {
        if (mcc$sections == null) {
            var out = new legacyconnector.v1_12_2.net.minecraft.world.chunk.storage.ExtendedBlockStorage[16];
            for (int i = 0; i < 16; i++) out[i] = new legacyconnector.v1_12_2.net.minecraft.world.chunk.storage.ExtendedBlockStorage(this, i << 4);
            mcc$sections = out;
        }
        var out = mcc$sections.clone();
        if (access != null) for (int i = 0; i < 16; i++) if (isEmptyBetween(i << 4, (i << 4) + 15)) out[i] = NULL_BLOCK_STORAGE;
        return out;
    }

    public legacyconnector.v1_12_2.net.minecraft.world.biome.Biome getBiome(BlockPos pos, legacyconnector.v1_12_2.net.minecraft.world.biome.BiomeProvider provider) {
        if (world != null && (access != null || world.mcc$level() != null)) return world.getBiome(pos);
        return provider == null ? legacyconnector.v1_12_2.net.minecraft.init.Biomes.PLAINS : provider.getBiome(pos, legacyconnector.v1_12_2.net.minecraft.init.Biomes.PLAINS);
    }

    // ------------------------------------------------------------------ area W: legacy fields

    /** Phantom: redirected to {@link #mcc$get$dirty} (whether the chunk has unsaved changes; setting it marks the chunk). */
    @Deprecated
    public boolean dirty;
    /** Phantom: redirected to {@link #mcc$get$storageArrays} (the 16 sections, see {@link #getBlockStorageArray}). */
    @Deprecated
    public legacyconnector.v1_12_2.net.minecraft.world.chunk.storage.ExtendedBlockStorage[] storageArrays;
    /** Phantom: redirected to {@link #mcc$get$precipitationHeightMap} (per column: the y where rain and snow land). */
    @Deprecated
    public int[] precipitationHeightMap;

    public static boolean mcc$get$dirty(Chunk c) {
        return c.needsSaving(false);
    }

    public static void mcc$set$dirty(Chunk c, boolean v) {
        if (v) c.markDirty();
    }

    public static legacyconnector.v1_12_2.net.minecraft.world.chunk.storage.ExtendedBlockStorage[] mcc$get$storageArrays(Chunk c) {
        return c.getBlockStorageArray();
    }

    public static int[] mcc$get$precipitationHeightMap(Chunk c) {
        int[] out = new int[256];
        for (int i = 0; i < 16; i++) {
            for (int j = 0; j < 16; j++) {
                out[j << 4 | i] = c.getPrecipitationHeight(new BlockPos((c.x << 4) + i, 0, (c.z << 4) + j)).getY();
            }
        }
        return out;
    }
}
