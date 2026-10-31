package legacyconnector.v1_7_10.net.minecraft.world.chunk;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import legacyconnector.v1_7_10.net.minecraft.block.Block;
import legacyconnector.v1_7_10.net.minecraft.tileentity.TileEntity;
import legacyconnector.v1_7_10.net.minecraft.world.ChunkCoordIntPair;
import legacyconnector.v1_7_10.net.minecraft.world.World;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.chunk.ChunkAccess;

/**
 * 1.7.10 chunk: a view of a modern chunk through its world (block, metadata and tile entity access by local
 * coordinates, biome ids per column, the modified flag).
 */
public class Chunk {
    public final int xPosition;
    public final int zPosition;
    public World worldObj;
    public boolean isChunkLoaded = true;
    public boolean isTerrainPopulated = true;
    public boolean isLightPopulated = true;
    /** Phantom: redirected to {@link #mcc$get$isModified}/{@link #mcc$set$isModified} (setting it saves edited biomes). */
    @Deprecated
    public boolean isModified;
    private boolean mcc$modified;
    public boolean hasEntities;
    /** Phantom: redirected to {@link #mcc$get$chunkTileEntityMap} (a world chunk's tile entities live in its block entities). */
    @Deprecated
    public Map<Object, TileEntity> chunkTileEntityMap;
    private final Map<Object, TileEntity> mcc$tiles = new HashMap<>();

    /**
     * 1.7.10's map of the chunk's tile entities. A generator's chunk has its own; for a world chunk it is a snapshot built from the block entities
     * (IC2 walks it on ChunkWatchEvent.Watch to send every tile its initial data: with an empty map no client ever received any).
     */
    public static Map<Object, TileEntity> mcc$get$chunkTileEntityMap(Chunk c) {
        if (c.mcc$blocks != null || c.modern == null || c.worldObj == null) return c.mcc$tiles;
        Map<Object, TileEntity> out = new HashMap<>();
        // a chunk fetched through the chunk source is an ImposterProtoChunk around the loaded chunk, which lists no block entities itself
        net.minecraft.world.level.chunk.ChunkAccess source = c.modern instanceof net.minecraft.world.level.chunk.ImposterProtoChunk imposter ? imposter.getWrapped() : c.modern;
        for (var pos : source.getBlockEntitiesPos()) {
            TileEntity te = c.worldObj.getTileEntity(pos.getX(), pos.getY(), pos.getZ());
            if (te != null) out.put(new legacyconnector.v1_7_10.net.minecraft.world.ChunkPosition(pos.getX() & 15, pos.getY(), pos.getZ() & 15), te);
        }
        return out;
    }
    @SuppressWarnings("unchecked")
    public List<Object>[] entityLists = new List[16];
    public int[] heightMap = new int[256];
    public int[] precipitationHeightMap = new int[256];
    public boolean[] updateSkylightColumns = new boolean[256];
    public int heightMapMinimum = Integer.MAX_VALUE;
    public boolean isLit;
    public boolean sendUpdates;
    public boolean isGapLightingUpdated;
    public int queuedLightChecks = 4096;
    public long lastSaveTime;
    public long inhabitedTime;

    /** Phantom: redirected to {@link #mcc$get$storageArrays}. */
    @Deprecated
    public legacyconnector.v1_7_10.net.minecraft.world.chunk.storage.ExtendedBlockStorage[] storageArrays;

    public static legacyconnector.v1_7_10.net.minecraft.world.chunk.storage.ExtendedBlockStorage[] mcc$get$storageArrays(Chunk c) {
        return c.getBlockStorageArray();
    }

    private final ChunkAccess modern;
    /** Array mode (a chunk a mod's generator builds): blocks as x << 12 | z << 8 | y, metadata by the same index. */
    private Block[] mcc$blocks;
    private byte[] mcc$meta;
    private byte[] mcc$biomes;
    /** For a live chunk: the biome ids {@link #getBiomeArray} handed out, to find the entries a mod changed. */
    private byte[] mcc$biomesRead;

    public static boolean mcc$get$isModified(Chunk c) {
        return c.mcc$modified;
    }

    /** Mods edit {@code getBiomeArray()} in place and then set {@code isModified} (Biome Wand): the edits reach the world here. */
    public static void mcc$set$isModified(Chunk c, boolean modified) {
        c.mcc$modified = modified;
        if (modified) c.mcc$flushBiomes();
    }

    /** While set on a thread, {@code new Chunk(world, x, z)} builds a detached array chunk (a generator's new chunk). */
    private static final ThreadLocal<Boolean> DETACHED = new ThreadLocal<>();

    public static void mcc$beginDetached() {
        DETACHED.set(Boolean.TRUE);
    }

    public static void mcc$endDetached() {
        DETACHED.remove();
    }

    public Chunk(World world, int x, int z) {
        this(world, x, z, null);
        if (DETACHED.get() != null) {
            // 1.7.10's fresh chunk: empty storage the generator fills (directly or through ExtendedBlockStorage).
            this.mcc$blocks = new Block[65536];
            this.mcc$meta = new byte[65536];
            this.mcc$height = 256;
            this.isTerrainPopulated = false;
            this.isLightPopulated = false;
        }
    }

    /**
     * Views of the same loaded chunk are equal: 1.7.10 had one Chunk object per loaded chunk and mods key maps by it
     * (CodeChickenLib's world extensions), while every event here builds a fresh view.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        return o instanceof Chunk c && modern != null && c.modern != null && c.worldObj == worldObj
                && c.xPosition == xPosition && c.zPosition == zPosition;
    }

    @Override
    public int hashCode() {
        return modern == null ? System.identityHashCode(this) : (xPosition * 31 + zPosition) * 31 + System.identityHashCode(worldObj);
    }

    public Chunk(World world, ChunkAccess modern) {
        this(world, modern.getPos().x, modern.getPos().z, modern);
    }

    /** A chunk of column arrays without metadata (the nether generator's 128-high chunks). */
    public Chunk(World world, Block[] blocks, int x, int z) {
        this(world, blocks, null, x, z);
    }

    /** A chunk of 256-high column arrays, as legacy terrain generators build them. */
    public Chunk(World world, Block[] blocks, byte[] meta, int x, int z) {
        this(world, x, z, null);
        this.mcc$blocks = blocks;
        this.mcc$height = Math.max(1, blocks.length / 256);
        this.mcc$meta = meta == null ? new byte[blocks.length] : meta;
        this.isTerrainPopulated = false;
        this.isLightPopulated = false;
    }

    public boolean mcc$hasArrays() {
        return mcc$blocks != null;
    }

    public Block[] mcc$blocks() {
        mcc$mergeStorage();
        return mcc$blocks;
    }

    public byte[] mcc$meta() {
        mcc$mergeStorage();
        return mcc$meta;
    }

    /** Sections a generator made with {@code new ExtendedBlockStorage} and stored in {@link #getBlockStorageArray()} become part of the arrays. */
    private void mcc$mergeStorage() {
        if (mcc$storage == null || mcc$blocks == null) return;
        for (var s : mcc$storage) {
            if (s != null && s.mcc$standalone()) s.mcc$mergeInto(mcc$blocks, mcc$meta);
        }
    }

    /** Column height of an array chunk: the arrays hold (x * 16 + z) * height + y (vanilla 256, some mods' 128). */
    private int mcc$height = 256;

    public int mcc$arrayHeight() {
        return mcc$height;
    }

    private int index(int x, int y, int z) {
        return ((x & 15) * 16 + (z & 15)) * mcc$height + y;
    }

    private Chunk(World world, int x, int z, ChunkAccess modern) {
        this.worldObj = world;
        this.xPosition = x;
        this.zPosition = z;
        this.modern = modern;
        for (int i = 0; i < entityLists.length; i++) entityLists[i] = new ArrayList<>();
    }

    public ChunkAccess mcc$modern() {
        return modern;
    }

    private int wx(int x) {
        return (xPosition << 4) + (x & 15);
    }

    private int wz(int z) {
        return (zPosition << 4) + (z & 15);
    }

    public boolean isAtLocation(int x, int z) {
        return x == xPosition && z == zPosition;
    }

    public Block getBlock(int x, int y, int z) {
        if (mcc$blocks != null) {
            if (y < 0 || y >= mcc$height) return legacyconnector.v1_7_10.net.minecraft.init.Blocks.air;
            Block b = mcc$blocks[index(x, y, z)];
            return b == null ? legacyconnector.v1_7_10.net.minecraft.init.Blocks.air : b;
        }
        return worldObj.getBlock(wx(x), y, wz(z));
    }

    public int getBlockMetadata(int x, int y, int z) {
        if (mcc$blocks != null) return y < 0 || y >= mcc$height ? 0 : mcc$meta[index(x, y, z)] & 15;
        return worldObj.getBlockMetadata(wx(x), y, wz(z));
    }

    public boolean setBlockMetadata(int x, int y, int z, int meta) {
        if (mcc$blocks != null) {
            if (y < 0 || y >= mcc$height) return false;
            mcc$meta[index(x, y, z)] = (byte) meta;
            return true;
        }
        return worldObj.setBlockMetadataWithNotify(wx(x), y, wz(z), meta, 2);
    }

    public boolean func_150807_a(int x, int y, int z, Block block, int meta) {
        if (mcc$blocks != null) {
            if (y < 0 || y >= mcc$height) return false;
            int i = index(x, y, z);
            mcc$blocks[i] = block;
            mcc$meta[i] = (byte) meta;
            return true;
        }
        return worldObj.setBlock(wx(x), y, wz(z), block, meta, 2);
    }

    public int getHeightValue(int x, int z) {
        if (mcc$blocks != null) {
            for (int y = mcc$height - 1; y > 0; y--) {
                Block b = mcc$blocks[index(x, y, z)];
                if (b != null && b != legacyconnector.v1_7_10.net.minecraft.init.Blocks.air) return y + 1;
            }
            return 0;
        }
        return worldObj.getHeightValue(wx(x), wz(z));
    }

    public int getTopFilledSegment() {
        if (modern == null) return 0;
        int top = modern.getHighestFilledSectionIndex();
        return top < 0 ? 0 : modern.getSectionYFromSectionIndex(top) << 4;
    }

    /**
     * Legacy biome id per column (z << 4 | x), from the modern biomes at the surface. For a live chunk the same array is
     * returned on every call, and changes a mod writes into it are applied by {@link #setChunkModified} or {@code isModified = true}.
     */
    public byte[] getBiomeArray() {
        if (mcc$biomes != null) return mcc$biomes;
        if (mcc$blocks != null) return mcc$biomes = new byte[256];
        byte[] ids = currentBiomeIds();
        if (mcc$liveChunk() != null) {
            mcc$biomes = ids;
            mcc$biomesRead = ids.clone();
        }
        return ids;
    }

    /** The loaded chunk behind this view (a full chunk handed out as a proto chunk is wrapped in an ImposterProtoChunk). */
    private net.minecraft.world.level.chunk.LevelChunk mcc$liveChunk() {
        if (modern instanceof net.minecraft.world.level.chunk.LevelChunk c) return c;
        if (modern instanceof net.minecraft.world.level.chunk.ImposterProtoChunk p) return p.getWrapped();
        return null;
    }

    private byte[] currentBiomeIds() {
        byte[] ids = new byte[256];
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int wx = wx(x), wz = wz(z);
                var biome = worldObj.getBiomeGenForCoords(wx, wz);
                ids[z << 4 | x] = (byte) (biome == null ? 1 : biome.biomeID);
            }
        }
        return ids;
    }

    public void setBiomeArray(byte[] biomes) {
        if (mcc$liveChunk() != null && mcc$biomesRead == null) mcc$biomesRead = currentBiomeIds();
        this.mcc$biomes = biomes;
        mcc$flushBiomes();
    }

    /**
     * Writes the columns whose legacy biome id changed into the modern chunk (every 4x4x4 biome cell that holds a changed
     * column, over the full height, the way /fillbiome does) and sends the new biomes to the players watching the chunk.
     */
    private void mcc$flushBiomes() {
        var chunk = mcc$liveChunk();
        if (chunk == null || mcc$biomes == null || mcc$biomesRead == null) return;
        if (!(chunk.getLevel() instanceof net.minecraft.server.level.ServerLevel level)) return;
        var registry = level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.BIOME);
        @SuppressWarnings("unchecked")
        net.minecraft.core.Holder<net.minecraft.world.level.biome.Biome>[] cells = new net.minecraft.core.Holder[16];
        boolean any = false;
        for (int i = 0; i < 256; i++) {
            if (mcc$biomes[i] == mcc$biomesRead[i]) continue;
            var legacy = legacyconnector.v1_7_10.net.minecraft.world.biome.BiomeGenBase.getBiome(mcc$biomes[i] & 255);
            var id = org.kylepitcock.legacyconnector.v1_7_10.bridge.LegacyEntitySpawns.modernBiome(legacy);
            if (id == null) continue;
            var holder = registry.getHolder(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.BIOME, id));
            if (holder.isEmpty()) continue;
            int x = i & 15, z = i >> 4;
            cells[(z >> 2) << 2 | (x >> 2)] = holder.get();
            any = true;
        }
        mcc$biomesRead = mcc$biomes.clone();
        if (!any) return;
        int baseX = net.minecraft.core.QuartPos.fromBlock(chunk.getPos().getMinBlockX());
        int baseZ = net.minecraft.core.QuartPos.fromBlock(chunk.getPos().getMinBlockZ());
        chunk.fillBiomesFromNoise((qx, qy, qz, sampler) -> {
            var replaced = cells[(qz - baseZ) << 2 | (qx - baseX)];
            return replaced != null ? replaced : chunk.getNoiseBiome(qx, qy, qz);
        }, level.getChunkSource().randomState().sampler());
        chunk.setUnsaved(true);
        level.getChunkSource().chunkMap.resendBiomesForChunks(java.util.List.of(chunk));
    }

    /**
     * The biome of a column (local coordinates): a generator's chunk answers from its biome array, filling an unset (255) entry from the
     * chunk manager as 1.7.10 did; a world chunk answers from the world.
     */
    public legacyconnector.v1_7_10.net.minecraft.world.biome.BiomeGenBase getBiomeGenForWorldCoords(int x, int z,
            legacyconnector.v1_7_10.net.minecraft.world.biome.WorldChunkManager chunkManager) {
        if (mcc$blocks != null || modern == null) {
            byte[] biomes = getBiomeArray();
            int i = (z & 15) << 4 | (x & 15);
            int id = biomes[i] & 255;
            if (id == 255 && chunkManager != null) {
                var biome = chunkManager.getBiomeGenAt(wx(x), wz(z));
                if (biome != null) {
                    biomes[i] = (byte) (biome.biomeID & 255);
                    return biome;
                }
            }
            if (id != 255 && legacyconnector.v1_7_10.net.minecraft.world.biome.BiomeGenBase.getBiomeGenArray()[id] != null) {
                return legacyconnector.v1_7_10.net.minecraft.world.biome.BiomeGenBase.getBiomeGenArray()[id];
            }
            if (chunkManager != null) {
                var biome = chunkManager.getBiomeGenAt(wx(x), wz(z));
                if (biome != null) return biome;
            }
            return legacyconnector.v1_7_10.net.minecraft.world.biome.BiomeGenBase.plains;
        }
        return worldObj.getBiomeGenForCoords(wx(x), wz(z));
    }

    public TileEntity func_150806_e(int x, int y, int z) {
        if (mcc$blocks != null || modern == null) return mcc$tiles.get(new legacyconnector.v1_7_10.net.minecraft.world.ChunkPosition(x & 15, y, z & 15));
        return worldObj.getTileEntity(wx(x), y, wz(z));
    }

    /** The tile entity at local coordinates (the 1.7.2 name of {@link #func_150806_e}). */
    public TileEntity getBlockTileEntityInChunk(int x, int y, int z) {
        return func_150806_e(x, y, z);
    }

    /**
     * Puts a tile entity into the chunk at its own coordinates: into the world for a world chunk, into the chunk's own map for a
     * generator's chunk.
     */
    public void addTileEntity(TileEntity te) {
        if (te == null) return;
        int x = te.xCoord - (xPosition << 4);
        int z = te.zCoord - (zPosition << 4);
        if (mcc$blocks != null || modern == null) {
            te.setWorldObj(worldObj);
            mcc$tiles.put(new legacyconnector.v1_7_10.net.minecraft.world.ChunkPosition(x & 15, te.yCoord, z & 15), te);
            return;
        }
        if (worldObj != null) worldObj.setTileEntity(te.xCoord, te.yCoord, te.zCoord, te);
    }

    /** Drops an invalidated tile entity from the chunk (a world chunk's own block entity handles this itself). */
    public void removeInvalidTileEntity(int x, int y, int z) {
        var key = new legacyconnector.v1_7_10.net.minecraft.world.ChunkPosition(x & 15, y, z & 15);
        TileEntity te = mcc$tiles.get(key);
        if (te != null && te.isInvalid()) mcc$tiles.remove(key);
    }

    /**
     * Decorates the chunk once its neighbours to the south-east exist (vanilla populated a chunk from the corner of four): the
     * generator's {@code populate} runs for each chunk of the square that is ready. World chunks are populated by the modern game.
     */
    public void populateChunk(legacyconnector.v1_7_10.net.minecraft.world.chunk.IChunkProvider provider,
            legacyconnector.v1_7_10.net.minecraft.world.chunk.IChunkProvider generator, int x, int z) {
        if (provider == null || generator == null) return;
        if (!isTerrainPopulated && provider.chunkExists(x + 1, z + 1) && provider.chunkExists(x, z + 1) && provider.chunkExists(x + 1, z)) {
            provider.populate(generator, x, z);
        }
        if (provider.chunkExists(x - 1, z) && !populated(provider, x - 1, z) && provider.chunkExists(x - 1, z + 1) && provider.chunkExists(x, z + 1)
                && provider.chunkExists(x - 1, z + 1)) {
            provider.populate(generator, x - 1, z);
        }
        if (provider.chunkExists(x, z - 1) && !populated(provider, x, z - 1) && provider.chunkExists(x + 1, z - 1) && provider.chunkExists(x + 1, z)) {
            provider.populate(generator, x, z - 1);
        }
        if (provider.chunkExists(x - 1, z - 1) && !populated(provider, x - 1, z - 1) && provider.chunkExists(x, z - 1) && provider.chunkExists(x - 1, z)) {
            provider.populate(generator, x - 1, z - 1);
        }
    }

    private static boolean populated(legacyconnector.v1_7_10.net.minecraft.world.chunk.IChunkProvider provider, int x, int z) {
        Chunk c = provider.provideChunk(x, z);
        return c == null || c.isTerrainPopulated;
    }

    /** Whether the chunk is fully generated: populated and lit. */
    public boolean func_150802_k() {
        return isTerrainPopulated && isLightPopulated;
    }

    /**
     * Light at local coordinates: sky light less {@code skylightSubtracted}, or block light if brighter. Generator chunks have no light
     * yet: full sky light above their terrain.
     */
    public int getBlockLightValue(int x, int y, int z, int skylightSubtracted) {
        if (worldObj == null || mcc$blocks != null || modern == null) return Math.max(0, 15 - skylightSubtracted);
        int sky = worldObj.getSkyBlockTypeBrightness(legacyconnector.v1_7_10.net.minecraft.world.EnumSkyBlock.Sky, wx(x), y, wz(z));
        int block = worldObj.getSkyBlockTypeBrightness(legacyconnector.v1_7_10.net.minecraft.world.EnumSkyBlock.Block, wx(x), y, wz(z));
        return Math.max(sky - skylightSubtracted, block);
    }

    /** The height rain and snow fall to in the column (local coordinates). */
    public int getPrecipitationHeight(int x, int z) {
        if (mcc$blocks != null || worldObj == null) return getHeightValue(x, z);
        return worldObj.getPrecipitationHeight(wx(x), wz(z));
    }

    /** Sky light of a column is propagated by the modern light engine. */
    public void propagateSkylightOcclusion(int x, int z) {
    }

    /** The block's light is recomputed (the modern engine relights it and its column). */
    public void relightBlock(int x, int y, int z) {
        if (worldObj != null && modern != null) worldObj.updateLightByType(legacyconnector.v1_7_10.net.minecraft.world.EnumSkyBlock.Sky, wx(x), y, wz(z));
    }

    /** Light is stored by the modern light engine, which computes it from the blocks; a value set here is not kept. */
    public void setLightValue(legacyconnector.v1_7_10.net.minecraft.world.EnumSkyBlock type, int x, int y, int z, int value) {
    }

    public TileEntity getTileEntityUnsafe(int x, int y, int z) {
        return func_150806_e(x, y, z);
    }

    public void removeTileEntity(int x, int y, int z) {
        worldObj.removeTileEntity(wx(x), y, wz(z));
    }

    public void setChunkModified() {
        mcc$modified = true;
        if (modern != null) modern.setUnsaved(true);
        mcc$flushBiomes();
    }

    public boolean needsSaving(boolean all) {
        return modern != null && modern.isUnsaved();
    }

    private legacyconnector.v1_7_10.net.minecraft.world.chunk.storage.ExtendedBlockStorage[] mcc$storage;

    /** Sixteen 16-block sections covering legacy heights 0-255, as views onto this chunk. */
    public legacyconnector.v1_7_10.net.minecraft.world.chunk.storage.ExtendedBlockStorage[] getBlockStorageArray() {
        if (mcc$storage == null) {
            mcc$storage = new legacyconnector.v1_7_10.net.minecraft.world.chunk.storage.ExtendedBlockStorage[16];
            for (int i = 0; i < 16; i++) mcc$storage[i] = new legacyconnector.v1_7_10.net.minecraft.world.chunk.storage.ExtendedBlockStorage(this, i << 4);
        }
        return mcc$storage;
    }

    public void setStorageArrays(legacyconnector.v1_7_10.net.minecraft.world.chunk.storage.ExtendedBlockStorage[] storage) {
    }

    public ChunkCoordIntPair getChunkCoordIntPair() {
        return new ChunkCoordIntPair(xPosition, zPosition);
    }

    public Random getRandomWithSeed(long seed) {
        return new Random(worldObj.getSeed() + (long) xPosition * xPosition * 4987142 + xPosition * 5947611L
                + (long) zPosition * zPosition * 4392871L + zPosition * 389711L ^ seed);
    }

    public boolean getAreLevelsEmpty(int minY, int maxY) {
        if (modern == null) return true;
        for (int y = minY; y <= maxY; y += 16) {
            if (y < modern.getMinBuildHeight() || y >= modern.getMaxBuildHeight()) continue;
            if (!modern.getSection(modern.getSectionIndex(y)).hasOnlyAir()) return false;
        }
        return true;
    }

    public boolean canBlockSeeTheSky(int x, int y, int z) {
        return worldObj.canBlockSeeTheSky(wx(x), y, wz(z));
    }

    public void generateSkylightMap() {
    }

    public void generateHeightMap() {
    }

    public void resetRelightChecks() {
    }

    public void enqueueRelightChecks() {
    }

    public void onChunkLoad() {
    }

    public void onChunkUnload() {
    }

    public boolean isEmpty() {
        return modern == null;
    }

    public BlockPos mcc$pos(int x, int y, int z) {
        return new BlockPos(wx(x), y, wz(z));
    }

    public void removeEntity(legacyconnector.v1_7_10.net.minecraft.entity.Entity entity) {
    }

    /** How much light the block at local coordinates absorbs ({@code func_150808_b}). */
    public int getBlockLightOpacity(int x, int y, int z) {
        Block block = getBlock(x, y, z);
        if (mcc$blocks != null || worldObj == null) return block.getLightOpacity();
        return block.getLightOpacity(worldObj, wx(x), y, wz(z));
    }

    /**
     * Adds the entity to the chunk's legacy entity lists. 1.7.10's {@code spawnEntityInWorld} called this; legacy code that
     * calls it directly expects the entity to join the world with the chunk, so an entity added to a world chunk (not to a
     * generator's own array chunk) is spawned (a no-op for one the world already has).
     */
    public void addEntity(legacyconnector.v1_7_10.net.minecraft.entity.Entity entity) {
        if (entity == null) return;
        hasEntities = true;
        int section = (int) Math.floor(legacyconnector.v1_7_10.net.minecraft.entity.Entity.mcc$get$posY(entity) / 16.0D);
        entityLists[Math.max(0, Math.min(entityLists.length - 1, section))].add(entity);
        if (mcc$blocks == null && modern != null && worldObj != null) worldObj.spawnEntityInWorld(entity);
    }

    public int getSavedLightValue(legacyconnector.v1_7_10.net.minecraft.world.EnumSkyBlock type, int x, int y, int z) {
        return worldObj.getSkyBlockTypeBrightness(type, wx(x), y, wz(z));
    }

    public boolean setBlockIDWithMetadata(int x, int y, int z, legacyconnector.v1_7_10.net.minecraft.block.Block block, int meta) {
        return worldObj.setBlock(wx(x), y, wz(z), block, meta, 2);
    }

    public void setBlockTileEntityInChunk(int x, int y, int z, legacyconnector.v1_7_10.net.minecraft.tileentity.TileEntity tile) {
        worldObj.setTileEntity(wx(x), y, wz(z), tile);
    }
}
