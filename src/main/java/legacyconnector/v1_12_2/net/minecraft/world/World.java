package legacyconnector.v1_12_2.net.minecraft.world;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import legacyconnector.v1_12_2.net.minecraft.block.Block;
import legacyconnector.v1_12_2.net.minecraft.block.material.Material;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.tileentity.TileEntity;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import legacyconnector.v1_12_2.net.minecraft.util.SoundCategory;
import legacyconnector.v1_12_2.net.minecraft.util.SoundEvent;
import legacyconnector.v1_12_2.net.minecraft.util.math.AxisAlignedBB;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.util.math.Vec3d;
import legacyconnector.v1_12_2.net.minecraft.world.biome.Biome;
import legacyconnector.v1_12_2.net.minecraft.world.chunk.Chunk;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.SignalGetter;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.EntityBridge1122;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.StatesBridge1122;

/**
 * The 1.12.2 World API as a view over a modern {@link LevelAccessor}: a live {@link Level} during play, or a generation region
 * while legacy world generators run (blocks are then read and written through the region only, and nothing reaches the live
 * level: no neighbour updates, sounds or entity searches). Coordinates are absolute, so legacy code sees the modern height range.
 */
public class World implements IBlockAccess {
    private final LevelAccessor access;
    private final Level level;

    public final boolean isRemote;
    public final Random rand = new Random();
    public final legacyconnector.v1_12_2.net.minecraft.profiler.Profiler profiler = new legacyconnector.v1_12_2.net.minecraft.profiler.Profiler();
    /** Forge: how far an entity's box may reach past its chunk. */
    public static double MAX_ENTITY_RADIUS = 2.0D;
    public boolean captureBlockSnapshots;
    public boolean restoringBlockSnapshots;
    public java.util.ArrayList<Object> capturedBlockSnapshots = new java.util.ArrayList<>();
    public final List<Object> eventListeners = new ArrayList<>();
    public final List<Entity> weatherEffects = new ArrayList<>();

    /** Phantom: redirected to {@link #mcc$get$provider} (created on first use). */
    @Deprecated
    public WorldProvider provider;
    private volatile WorldProvider mcc$provider;
    /** Phantom: redirected to {@link #mcc$get$playerEntities}. */
    @Deprecated
    public List<EntityPlayer> playerEntities;
    /** Phantom: redirected to {@link #mcc$get$loadedEntityList}. */
    @Deprecated
    public List<Entity> loadedEntityList;
    /** Phantom: redirected to {@link #mcc$get$loadedTileEntityList}. */
    @Deprecated
    public List<TileEntity> loadedTileEntityList;
    /** Phantom: redirected to {@link #mcc$get$tickableTileEntities}. */
    @Deprecated
    public List<TileEntity> tickableTileEntities;
    /** Phantom: redirected to {@link #mcc$get$worldInfo}. */
    @Deprecated
    public legacyconnector.v1_12_2.net.minecraft.world.storage.WorldInfo worldInfo;
    /** Phantom: redirected to {@link #mcc$get$saveHandler}. */
    @Deprecated
    public legacyconnector.v1_12_2.net.minecraft.world.storage.ISaveHandler saveHandler;
    /** Phantom: redirected to {@link #mcc$get$chunkProvider}. */
    @Deprecated
    public legacyconnector.v1_12_2.net.minecraft.world.chunk.IChunkProvider chunkProvider;
    /** Phantom: redirected to {@link #mcc$get$mapStorage}. */
    @Deprecated
    public legacyconnector.v1_12_2.net.minecraft.world.storage.MapStorage mapStorage;
    /** Phantom: redirected to {@link #mcc$get$rainingStrength}. */
    @Deprecated
    public float rainingStrength;
    /** Phantom: redirected to {@link #mcc$get$prevRainingStrength}. */
    @Deprecated
    public float prevRainingStrength;
    /** Phantom: redirected to {@link #mcc$get$thunderingStrength}. */
    @Deprecated
    public float thunderingStrength;
    /** Phantom: redirected to {@link #mcc$get$prevThunderingStrength}. */
    @Deprecated
    public float prevThunderingStrength;

    private int seaLevel = 63;
    private int lastLightningBolt;
    private legacyconnector.v1_12_2.net.minecraft.world.gen.ChunkProviderServer mcc$chunkProvider;
    private legacyconnector.v1_12_2.net.minecraft.world.storage.WorldInfo mcc$ownInfo;

    protected World(Level level) {
        this((LevelAccessor) level);
    }

    protected World(LevelAccessor access) {
        this.access = access;
        this.level = access instanceof Level l ? l : access instanceof WorldGenLevel g ? g.getLevel() : null;
        this.isRemote = access != null && access.isClientSide();
        if (level != null) this.seaLevel = level.getSeaLevel();
    }

    /** A world a mod builds itself (a fake world for rendering or simulation): no level behind it, every query sees empty space. */
    protected World(legacyconnector.v1_12_2.net.minecraft.world.storage.ISaveHandler saveHandler, legacyconnector.v1_12_2.net.minecraft.world.storage.WorldInfo info,
            WorldProvider provider, legacyconnector.v1_12_2.net.minecraft.profiler.Profiler profiler, boolean client) {
        this.access = null;
        this.level = null;
        this.isRemote = client;
        this.mcc$provider = provider;
        this.mcc$ownInfo = info;
    }

    // ------------------------------------------------------------------ the modern level

    /** The full level (also for a generation region's view), or null for a detached world. */
    public Level mcc$level() {
        return level;
    }

    /** What block reads and writes go through: the level, or the generation region. */
    public LevelAccessor mcc$access() {
        return access;
    }

    /** The level for updates, sounds and entities; null while this world is a generation region (those calls would deadlock). */
    public Level mcc$liveLevel() {
        return access instanceof Level l ? l : null;
    }

    private ServerLevel serverLevel() {
        return access instanceof ServerLevel s ? s : null;
    }

    private static net.minecraft.core.BlockPos modern(BlockPos p) {
        return new net.minecraft.core.BlockPos(p.getX(), p.getY(), p.getZ());
    }

    private static net.minecraft.world.level.block.Block modernBlock(Block block) {
        return block != null && block.mcc$modern instanceof net.minecraft.world.level.block.Block b ? b : net.minecraft.world.level.block.Blocks.AIR;
    }

    /**
     * Views of one level are the same world (1.12 had one World per dimension): generation hands out a view per region, and mods
     * keep per-world data in hash maps.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        return o instanceof World other && level != null && level == other.level;
    }

    @Override
    public int hashCode() {
        return level != null ? System.identityHashCode(level) : System.identityHashCode(this);
    }

    // ------------------------------------------------------------------ virtual fields

    public static WorldProvider mcc$get$provider(World w) {
        WorldProvider p = w.mcc$provider;
        if (p != null) return p;
        synchronized (w) {
            if (w.mcc$provider == null) {
                if (w.level == null) {
                    WorldProviderSurface surface = new WorldProviderSurface();
                    w.mcc$provider = surface;
                    try {
                        surface.setWorld(w);
                    } catch (RuntimeException | LinkageError e) {
                        // a detached world's provider stays bare
                    }
                } else {
                    w.mcc$provider = WorldProvider.mcc$forLevel(w.level, w);
                }
            }
            return w.mcc$provider;
        }
    }

    public static void mcc$set$provider(World w, WorldProvider provider) {
        w.mcc$provider = provider;
    }

    public static List<EntityPlayer> mcc$get$playerEntities(World w) {
        List<EntityPlayer> out = new ArrayList<>();
        if (w.level == null) return out;
        for (var p : w.level.players()) {
            if (EntityBridge1122.wrap(p) instanceof EntityPlayer legacy) out.add(legacy);
        }
        return out;
    }

    public static List<Entity> mcc$get$loadedEntityList(World w) {
        List<Entity> out = new ArrayList<>();
        if (w.mcc$liveLevel() instanceof ServerLevel s) {
            for (var e : s.getAllEntities()) out.add(EntityBridge1122.wrap(e));
        } else if (w.mcc$liveLevel() != null && w.mcc$liveLevel().isClientSide()) {
            for (var e : org.kylepitcock.legacyconnector.v1_12_2.bridge.WorldClient1122.entities(w.mcc$liveLevel())) out.add(EntityBridge1122.wrap(e));
        }
        return out;
    }

    /** The legacy tile entities of the loaded chunks (a snapshot). */
    public static List<TileEntity> mcc$get$loadedTileEntityList(World w) {
        List<TileEntity> out = new ArrayList<>();
        if (w.serverLevel() == null) return out;
        for (var holder : w.serverLevel().getChunkSource().chunkMap.getChunks()) {
            var chunk = holder.getTickingChunk();
            if (chunk == null) continue;
            for (var be : chunk.getBlockEntities().values()) {
                if (be instanceof org.kylepitcock.legacyconnector.v1_12_2.adapter.Legacy1122BlockEntity l && l.legacy() != null) out.add(l.legacy());
            }
        }
        return out;
    }

    public static List<TileEntity> mcc$get$tickableTileEntities(World w) {
        List<TileEntity> out = new ArrayList<>();
        for (TileEntity te : mcc$get$loadedTileEntityList(w)) {
            if (te instanceof legacyconnector.v1_12_2.net.minecraft.util.ITickable) out.add(te);
        }
        return out;
    }

    public static legacyconnector.v1_12_2.net.minecraft.world.storage.WorldInfo mcc$get$worldInfo(World w) {
        if (w.level != null) return legacyconnector.v1_12_2.net.minecraft.world.storage.WorldInfo.mcc$of(w.level);
        if (w.mcc$ownInfo == null) w.mcc$ownInfo = legacyconnector.v1_12_2.net.minecraft.world.storage.WorldInfo.mcc$of(null);
        return w.mcc$ownInfo;
    }

    public static void mcc$set$worldInfo(World w, legacyconnector.v1_12_2.net.minecraft.world.storage.WorldInfo info) {
        if (w.level == null) w.mcc$ownInfo = info;
    }

    public static legacyconnector.v1_12_2.net.minecraft.world.storage.ISaveHandler mcc$get$saveHandler(World w) {
        return legacyconnector.v1_12_2.net.minecraft.world.storage.SaveHandler.mcc$of(w.level);
    }

    public static legacyconnector.v1_12_2.net.minecraft.world.chunk.IChunkProvider mcc$get$chunkProvider(World w) {
        return w.getChunkProvider();
    }

    public static legacyconnector.v1_12_2.net.minecraft.world.storage.MapStorage mcc$get$mapStorage(World w) {
        return w.getMapStorage();
    }

    public static float mcc$get$rainingStrength(World w) {
        return w.level == null ? 0.0F : w.level.getRainLevel(1.0F);
    }

    public static void mcc$set$rainingStrength(World w, float value) {
        w.setRainStrength(value);
    }

    public static float mcc$get$prevRainingStrength(World w) {
        return w.level == null ? 0.0F : w.level.getRainLevel(0.0F);
    }

    public static float mcc$get$thunderingStrength(World w) {
        return w.level == null ? 0.0F : w.level.getThunderLevel(1.0F);
    }

    public static void mcc$set$thunderingStrength(World w, float value) {
        w.setThunderStrength(value);
    }

    public static float mcc$get$prevThunderingStrength(World w) {
        return w.level == null ? 0.0F : w.level.getThunderLevel(0.0F);
    }

    // ------------------------------------------------------------------ blocks

    /**
     * A server level that has not loaded the chunk: 1.12 read such blocks as air, while the modern level would load the chunk and
     * wait for it (a deadlock when asked from another thread or while the chunk unloads).
     */
    private boolean unloaded(net.minecraft.core.BlockPos p) {
        return access instanceof ServerLevel sl && !sl.getChunkSource().hasChunk(p.getX() >> 4, p.getZ() >> 4);
    }

    /** A block read off the server thread: only a chunk that is fully loaded right now is read. Null on the server thread. */
    private BlockState offThreadState(net.minecraft.core.BlockPos p) {
        if (!(access instanceof ServerLevel sl) || sl.getServer().isSameThread()) return null;
        var holder = sl.getChunkSource().chunkMap.getVisibleChunkIfPresent(net.minecraft.world.level.ChunkPos.asLong(p.getX() >> 4, p.getZ() >> 4));
        var chunk = holder == null ? null : holder.getChunkIfPresent(net.minecraft.world.level.chunk.status.ChunkStatus.FULL);
        return chunk == null ? net.minecraft.world.level.block.Blocks.AIR.defaultBlockState() : chunk.getBlockState(p);
    }

    /** The modern state at a position as legacy code may read it (air where it is not available). */
    public BlockState mcc$state(net.minecraft.core.BlockPos p) {
        if (access == null || unloaded(p)) return net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
        if (access instanceof net.minecraft.server.level.WorldGenRegion r && !r.hasChunk(p.getX() >> 4, p.getZ() >> 4)) {
            return net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
        }
        BlockState off = offThreadState(p);
        return off != null ? off : access.getBlockState(p);
    }

    /** Legacy generators spill past the chunk being decorated; a generation region only accepts writes within one chunk of its centre. */
    private boolean outsideRegion(net.minecraft.core.BlockPos p) {
        if (access instanceof net.minecraft.server.level.WorldGenRegion r) {
            var c = r.getCenter();
            return Math.abs((p.getX() >> 4) - c.x) > 1 || Math.abs((p.getZ() >> 4) - c.z) > 1;
        }
        return access == null;
    }

    @Override
    public IBlockState getBlockState(BlockPos pos) {
        IBlockState state = StatesBridge1122.toLegacy(mcc$state(modern(pos)));
        if (state == null) state = org.kylepitcock.legacyconnector.v1_12_2.bridge.Vanilla1122.airState();
        return state;
    }

    public boolean setBlockState(BlockPos pos, IBlockState state) {
        return setBlockState(pos, state, 3);
    }

    public boolean setBlockState(BlockPos pos, IBlockState state, int flags) {
        var p = modern(pos);
        if (outsideRegion(p) || isOutsideBuildHeight(pos)) return false;
        BlockState modernState = StatesBridge1122.toModern(state);
        if (access instanceof net.minecraft.server.level.WorldGenRegion region) {
            boolean placed = region.setBlock(p, modernState, flags & ~1);
            if (placed) {
                region.getChunk(p).markPosForPostprocessing(p);
                ensureGeneratedBlockEntity(region, p, modernState);
            }
            return placed;
        }
        return access.setBlock(p, modernState, flags);
    }

    /** Places a modern state (vanilla template blocks keep their properties this way), with the same rules as {@link #setBlockState}. */
    public boolean mcc$setModernState(BlockPos pos, BlockState modernState, int flags) {
        var p = modern(pos);
        if (outsideRegion(p) || isOutsideBuildHeight(pos)) return false;
        if (access instanceof net.minecraft.server.level.WorldGenRegion region) {
            boolean placed = region.setBlock(p, modernState, flags & ~1);
            if (placed) {
                region.getChunk(p).markPosForPostprocessing(p);
                ensureGeneratedBlockEntity(region, p, modernState);
            }
            return placed;
        }
        return access.setBlock(p, modernState, flags);
    }

    /** Generation only records a stub for a placed block entity, but legacy generators fill theirs right away (chest loot, spawners). */
    private static void ensureGeneratedBlockEntity(net.minecraft.server.level.WorldGenRegion region, net.minecraft.core.BlockPos p, BlockState state) {
        if (!state.hasBlockEntity() || !(state.getBlock() instanceof net.minecraft.world.level.block.EntityBlock entityBlock)) return;
        var chunk = region.getChunk(p);
        if (chunk.getBlockEntity(p) != null) return;
        var be = entityBlock.newBlockEntity(p, state);
        if (be != null) chunk.setBlockEntity(be);
    }

    public boolean setBlockToAir(BlockPos pos) {
        return setBlockState(pos, org.kylepitcock.legacyconnector.v1_12_2.bridge.Vanilla1122.airState(), 3);
    }

    public boolean destroyBlock(BlockPos pos, boolean dropBlock) {
        var p = modern(pos);
        if (outsideRegion(p)) return false;
        return access.destroyBlock(p, dropBlock);
    }

    @Override
    public boolean isAirBlock(BlockPos pos) {
        return mcc$state(modern(pos)).isAir();
    }

    public boolean isBlockLoaded(BlockPos pos) {
        return isBlockLoaded(pos, true);
    }

    public boolean isBlockLoaded(BlockPos pos, boolean allowEmpty) {
        if (access == null || isOutsideBuildHeight(pos)) return false;
        if (access instanceof ServerLevel sl) return sl.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4) != null;
        return access.hasChunk(pos.getX() >> 4, pos.getZ() >> 4);
    }

    public boolean isAreaLoaded(BlockPos center, int radius) {
        return isAreaLoaded(center, radius, true);
    }

    public boolean isAreaLoaded(BlockPos center, int radius, boolean allowEmpty) {
        return isAreaLoaded(center.getX() - radius, center.getY() - radius, center.getZ() - radius, center.getX() + radius, center.getY() + radius,
                center.getZ() + radius, allowEmpty);
    }

    public boolean isAreaLoaded(BlockPos from, BlockPos to) {
        return isAreaLoaded(from, to, true);
    }

    public boolean isAreaLoaded(BlockPos from, BlockPos to, boolean allowEmpty) {
        return isAreaLoaded(from.getX(), from.getY(), from.getZ(), to.getX(), to.getY(), to.getZ(), allowEmpty);
    }

    public boolean isAreaLoaded(legacyconnector.v1_12_2.net.minecraft.world.gen.structure.StructureBoundingBox box) {
        return isAreaLoaded(box, true);
    }

    public boolean isAreaLoaded(legacyconnector.v1_12_2.net.minecraft.world.gen.structure.StructureBoundingBox box, boolean allowEmpty) {
        return isAreaLoaded(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ, allowEmpty);
    }

    public boolean isAreaLoaded(int x1, int y1, int z1, int x2, int y2, int z2, boolean allowEmpty) {
        if (access == null || y2 < access.getMinBuildHeight() || y1 >= access.getMaxBuildHeight()) return false;
        for (int cx = x1 >> 4; cx <= x2 >> 4; cx++) {
            for (int cz = z1 >> 4; cz <= z2 >> 4; cz++) {
                boolean has = access instanceof ServerLevel sl ? sl.getChunkSource().getChunkNow(cx, cz) != null : access.hasChunk(cx, cz);
                if (!has) return false;
            }
        }
        return true;
    }

    public boolean isOutsideBuildHeight(BlockPos pos) {
        return access == null ? pos.getY() < 0 || pos.getY() >= 256 : access.isOutsideBuildHeight(pos.getY());
    }

    public boolean isValid(BlockPos pos) {
        return !isOutsideBuildHeight(pos) && pos.getX() >= -30000000 && pos.getZ() >= -30000000 && pos.getX() < 30000000 && pos.getZ() < 30000000;
    }

    public boolean isBlockNormalCube(BlockPos pos, boolean _default) {
        var p = modern(pos);
        if (access == null || !isBlockLoaded(pos)) return _default;
        return mcc$state(p).isRedstoneConductor(access, p);
    }

    public boolean isBlockFullCube(BlockPos pos) {
        var p = modern(pos);
        return access != null && mcc$state(p).isCollisionShapeFullBlock(access, p);
    }

    public boolean isSideSolid(BlockPos pos, EnumFacing side) {
        return isSideSolid(pos, side, false);
    }

    @Override
    public boolean isSideSolid(BlockPos pos, EnumFacing side, boolean _default) {
        var p = modern(pos);
        if (access == null || !isBlockLoaded(pos)) return _default;
        return mcc$state(p).isFaceSturdy(access, p, side.mcc$modern());
    }

    public boolean isBlockModifiable(EntityPlayer player, BlockPos pos) {
        return mcc$get$provider(this).canMineBlock(player, pos);
    }

    /** Vanilla's check: spawn protection and the world border. */
    public boolean canMineBlockBody(EntityPlayer player, BlockPos pos) {
        ServerLevel sl = serverLevel();
        var modernPlayer = player == null ? null : player.mcc$player();
        if (sl == null || modernPlayer == null) return true;
        return !sl.getServer().isUnderSpawnProtection(sl, modern(pos), modernPlayer) && sl.getWorldBorder().isWithinBounds(modern(pos));
    }

    /**
     * Height of the first block that stops motion or holds fluid, scanning down. In a generation region only the generation
     * heightmaps exist, so the surface is found from the blocks themselves.
     */
    private int surface(int x, int z, Heightmap.Types type) {
        if (access == null) return 0;
        if (access instanceof net.minecraft.server.level.WorldGenRegion region) {
            if (!region.hasChunk(x >> 4, z >> 4)) return region.getSeaLevel();
            net.minecraft.core.BlockPos.MutableBlockPos p = new net.minecraft.core.BlockPos.MutableBlockPos();
            boolean noLeaves = type == Heightmap.Types.MOTION_BLOCKING_NO_LEAVES;
            for (int y = region.getMaxBuildHeight() - 1; y >= region.getMinBuildHeight(); y--) {
                var state = region.getBlockState(p.set(x, y, z));
                if (state.isAir()) continue;
                if (noLeaves && state.getBlock() instanceof net.minecraft.world.level.block.LeavesBlock) continue;
                if (type == Heightmap.Types.WORLD_SURFACE || state.blocksMotion() || !state.getFluidState().isEmpty()) return y + 1;
            }
            return region.getMinBuildHeight();
        }
        if (unloaded(new net.minecraft.core.BlockPos(x, 0, z))) return 0;
        return access.getHeight(type, x, z);
    }

    public BlockPos getHeight(BlockPos pos) {
        return new BlockPos(pos.getX(), getHeight(pos.getX(), pos.getZ()), pos.getZ());
    }

    public int getHeight(int x, int z) {
        if (x < -30000000 || z < -30000000 || x >= 30000000 || z >= 30000000) return seaLevel + 1;
        return surface(x, z, Heightmap.Types.WORLD_SURFACE);
    }

    public int getChunksLowestHorizon(int x, int z) {
        Chunk c = getChunk(x >> 4, z >> 4);
        return c == null ? 0 : c.getLowestHeight();
    }

    public BlockPos getTopSolidOrLiquidBlock(BlockPos pos) {
        return new BlockPos(pos.getX(), surface(pos.getX(), pos.getZ(), Heightmap.Types.MOTION_BLOCKING_NO_LEAVES), pos.getZ());
    }

    public BlockPos getPrecipitationHeight(BlockPos pos) {
        return new BlockPos(pos.getX(), surface(pos.getX(), pos.getZ(), Heightmap.Types.MOTION_BLOCKING), pos.getZ());
    }

    public IBlockState getGroundAboveSeaLevel(BlockPos pos) {
        BlockPos p = new BlockPos(pos.getX(), getSeaLevel(), pos.getZ());
        while (!isAirBlock(p.up()) && p.getY() < 320) p = p.up();
        return getBlockState(p);
    }

    public boolean canSeeSky(BlockPos pos) {
        return access != null && !unloaded(modern(pos)) && access.canSeeSky(modern(pos));
    }

    public boolean canBlockSeeSky(BlockPos pos) {
        return canSeeSky(pos);
    }

    public boolean containsAnyLiquid(AxisAlignedBB bb) {
        return access != null && access.containsAnyLiquid(bb.mcc$modern());
    }

    public boolean isMaterialInBB(AxisAlignedBB bb, Material material) {
        if (access == null) return false;
        var box = bb.mcc$modern();
        var p = new net.minecraft.core.BlockPos.MutableBlockPos();
        for (int x = (int) Math.floor(box.minX); x < Math.ceil(box.maxX); x++) {
            for (int y = (int) Math.floor(box.minY); y < Math.ceil(box.maxY); y++) {
                for (int z = (int) Math.floor(box.minZ); z < Math.ceil(box.maxZ); z++) {
                    p.set(x, y, z);
                    IBlockState s = StatesBridge1122.toLegacy(mcc$state(p));
                    if (s != null && s.getMaterial() == material) return true;
                }
            }
        }
        return false;
    }

    public boolean isFlammableWithin(AxisAlignedBB bb) {
        if (access == null) return false;
        var box = bb.mcc$modern();
        var p = new net.minecraft.core.BlockPos.MutableBlockPos();
        for (int x = (int) Math.floor(box.minX); x < Math.ceil(box.maxX); x++) {
            for (int y = (int) Math.floor(box.minY); y < Math.ceil(box.maxY); y++) {
                for (int z = (int) Math.floor(box.minZ); z < Math.ceil(box.maxZ); z++) {
                    var s = mcc$state(p.set(x, y, z));
                    if (s.is(net.minecraft.tags.BlockTags.FIRE) || s.is(net.minecraft.world.level.block.Blocks.LAVA)) return true;
                }
            }
        }
        return false;
    }

    /** Whether the box touches a block of the material (1.12 also pushed the entity along the flow; the modern entity does that itself). */
    public boolean handleMaterialAcceleration(AxisAlignedBB bb, Material material, Entity entity) {
        return isMaterialInBB(bb, material);
    }

    public boolean collidesWithAnyBlock(AxisAlignedBB bb) {
        return access != null && access.getBlockCollisions(null, bb.mcc$modern()).iterator().hasNext();
    }

    public boolean checkBlockCollision(AxisAlignedBB bb) {
        if (access == null) return false;
        var box = bb.mcc$modern();
        var p = new net.minecraft.core.BlockPos.MutableBlockPos();
        for (int x = (int) Math.floor(box.minX); x < Math.ceil(box.maxX); x++) {
            for (int y = (int) Math.floor(box.minY); y < Math.ceil(box.maxY); y++) {
                for (int z = (int) Math.floor(box.minZ); z < Math.ceil(box.maxZ); z++) {
                    if (!mcc$state(p.set(x, y, z)).isAir()) return true;
                }
            }
        }
        return false;
    }

    /** Boxes of the blocks colliding with {@code bb} (and the world border, as 1.12 added it). */
    public List<AxisAlignedBB> getCollisionBoxes(Entity entity, AxisAlignedBB bb) {
        List<AxisAlignedBB> out = new ArrayList<>();
        if (access == null) return out;
        var modernEntity = entity == null ? null : entity.mcc$modern();
        for (var shape : access.getBlockCollisions(modernEntity, bb.mcc$modern())) {
            for (var part : shape.toAabbs()) out.add(AxisAlignedBB.mcc$of(part));
        }
        var bus = legacyconnector.v1_12_2.net.minecraftforge.common.MinecraftForge.EVENT_BUS;
        if (bus.mcc$hasListeners(legacyconnector.v1_12_2.net.minecraftforge.event.world.GetCollisionBoxesEvent.class)) {
            try {
                bus.post(new legacyconnector.v1_12_2.net.minecraftforge.event.world.GetCollisionBoxesEvent(this, entity, bb, out));
            } catch (RuntimeException | LinkageError e) {
                // the boxes as they are
            }
        }
        return out;
    }

    /** Whether the block may be placed (replaceable target, no entity in the way unless skipped). */
    public boolean mayPlace(Block block, BlockPos pos, boolean skipCollisionCheck, EnumFacing sidePlacedOn, Entity placer) {
        if (access == null) return false;
        var p = modern(pos);
        BlockState here = mcc$state(p);
        BlockState placing = modernBlock(block).defaultBlockState();
        if (!skipCollisionCheck) {
            var shape = placing.getCollisionShape(access, p);
            if (!shape.isEmpty() && !access.isUnobstructed(placer == null ? null : placer.mcc$modern(), shape.move(p.getX(), p.getY(), p.getZ()))) return false;
        }
        IBlockState legacyHere = StatesBridge1122.toLegacy(here);
        if (legacyHere != null && legacyHere.getMaterial() != null && legacyHere.getMaterial().isReplaceable()) return true;
        return here.canBeReplaced();
    }

    public boolean extinguishFire(EntityPlayer player, BlockPos pos, EnumFacing side) {
        BlockPos fire = pos.offset(side);
        var p = modern(fire);
        if (access != null && mcc$state(p).is(net.minecraft.tags.BlockTags.FIRE)) {
            playEvent(player, 1009, fire, 0);
            setBlockToAir(fire);
            return true;
        }
        return false;
    }

    public float getBlockDensity(Vec3d vec, AxisAlignedBB bb) {
        if (access == null) return 0.0F;
        double dx = 1.0D / ((bb.maxX - bb.minX) * 2.0D + 1.0D);
        double dy = 1.0D / ((bb.maxY - bb.minY) * 2.0D + 1.0D);
        double dz = 1.0D / ((bb.maxZ - bb.minZ) * 2.0D + 1.0D);
        double ox = (1.0D - Math.floor(1.0D / dx) * dx) / 2.0D;
        double oz = (1.0D - Math.floor(1.0D / dz) * dz) / 2.0D;
        if (dx < 0.0D || dy < 0.0D || dz < 0.0D) return 0.0F;
        int clear = 0;
        int total = 0;
        var to = vec.mcc$modern();
        for (float fx = 0.0F; fx <= 1.0F; fx = (float) (fx + dx)) {
            for (float fy = 0.0F; fy <= 1.0F; fy = (float) (fy + dy)) {
                for (float fz = 0.0F; fz <= 1.0F; fz = (float) (fz + dz)) {
                    var from = new net.minecraft.world.phys.Vec3(bb.minX + (bb.maxX - bb.minX) * fx + ox, bb.minY + (bb.maxY - bb.minY) * fy,
                            bb.minZ + (bb.maxZ - bb.minZ) * fz + oz);
                    var hit = access.clip(new net.minecraft.world.level.ClipContext(from, to, net.minecraft.world.level.ClipContext.Block.COLLIDER,
                            net.minecraft.world.level.ClipContext.Fluid.NONE, net.minecraft.world.phys.shapes.CollisionContext.empty()));
                    if (hit.getType() == net.minecraft.world.phys.HitResult.Type.MISS) clear++;
                    total++;
                }
            }
        }
        return total == 0 ? 0.0F : (float) clear / total;
    }

    // ------------------------------------------------------------------ IBlockAccess

    @Override
    public int getCombinedLight(BlockPos pos, int lightValue) {
        if (level == null || access instanceof net.minecraft.server.level.WorldGenRegion) return 15 << 20 | lightValue << 4;
        var p = modern(pos);
        int sky = level.getBrightness(net.minecraft.world.level.LightLayer.SKY, p);
        int block = Math.max(lightValue, level.getBrightness(net.minecraft.world.level.LightLayer.BLOCK, p));
        return sky << 20 | block << 4;
    }

    @Override
    public Biome getBiome(BlockPos pos) {
        if (access == null) return legacyconnector.v1_12_2.net.minecraft.init.Biomes.PLAINS;
        var p = modern(pos);
        boolean loaded = access instanceof net.minecraft.server.level.WorldGenRegion r ? r.getCenter().x == p.getX() >> 4 && r.getCenter().z == p.getZ() >> 4
                : access instanceof ServerLevel sl ? sl.getChunkSource().getChunkNow(p.getX() >> 4, p.getZ() >> 4) != null : access.hasChunk(p.getX() >> 4, p.getZ() >> 4);
        if (!loaded || access instanceof ServerLevel sl2 && !sl2.getServer().isSameThread()) {
            Biome generated = mcc$generatorBiome(pos.getX(), pos.getY(), pos.getZ());
            if (generated != null) return generated;
        }
        try {
            if (access instanceof net.minecraft.server.level.WorldGenRegion) {
                return org.kylepitcock.legacyconnector.v1_12_2.bridge.BiomeBridge1122.legacy(access.getNoiseBiome(p.getX() >> 2, p.getY() >> 2, p.getZ() >> 2));
            }
            return org.kylepitcock.legacyconnector.v1_12_2.bridge.BiomeBridge1122.legacy(access.getBiome(p));
        } catch (RuntimeException e) {
            Biome generated = mcc$generatorBiome(pos.getX(), pos.getY(), pos.getZ());
            return generated != null ? generated : legacyconnector.v1_12_2.net.minecraft.init.Biomes.PLAINS;
        }
    }

    /** The biome the dimension's generator places at a position (any coordinate, no chunk needed), or null. */
    public Biome mcc$generatorBiome(int x, int y, int z) {
        try {
            if (level instanceof ServerLevel sl) {
                var cache = sl.getChunkSource();
                return org.kylepitcock.legacyconnector.v1_12_2.bridge.BiomeBridge1122.legacy(
                        cache.getGenerator().getBiomeSource().getNoiseBiome(x >> 2, y >> 2, z >> 2, cache.randomState().sampler()));
            }
            if (level != null && level.hasChunk(x >> 4, z >> 4)) {
                return org.kylepitcock.legacyconnector.v1_12_2.bridge.BiomeBridge1122.legacy(level.getBiome(new net.minecraft.core.BlockPos(x, y, z)));
            }
        } catch (RuntimeException e) {
            // no biome
        }
        return null;
    }

    public Biome getBiomeForCoordsBody(BlockPos pos) {
        return getBiome(pos);
    }

    @Override
    public int getStrongPower(BlockPos pos, EnumFacing direction) {
        if (access == null) return 0;
        var p = modern(pos);
        return mcc$state(p).getDirectSignal(access, p, direction.mcc$modern());
    }

    public int getStrongPower(BlockPos pos) {
        return access instanceof SignalGetter s ? s.getDirectSignalTo(modern(pos)) : 0;
    }

    @Override
    public WorldType getWorldType() {
        return WorldType.mcc$of(access);
    }

    // ------------------------------------------------------------------ light

    public int getLight(BlockPos pos) {
        return level == null || access instanceof net.minecraft.server.level.WorldGenRegion ? 15 : level.getMaxLocalRawBrightness(modern(pos));
    }

    public int getLight(BlockPos pos, boolean checkNeighbors) {
        if (!checkNeighbors) return getLight(pos);
        return getLightFromNeighbors(pos);
    }

    /** The highest light of the block and (for slabs, stairs, farmland: blocks that use neighbour brightness) its neighbours. */
    public int getLightFromNeighbors(BlockPos pos) {
        if (level == null || access instanceof net.minecraft.server.level.WorldGenRegion) return 15;
        var p = modern(pos);
        int best = level.getMaxLocalRawBrightness(p);
        if (mcc$state(p).useShapeForLightOcclusion()) {
            for (Direction d : Direction.values()) best = Math.max(best, level.getMaxLocalRawBrightness(p.relative(d)));
        }
        return best;
    }

    public int getLightFor(EnumSkyBlock type, BlockPos pos) {
        if (level == null || access instanceof net.minecraft.server.level.WorldGenRegion) return type.defaultLightValue;
        return level.getBrightness(type.mcc$modern(), modern(pos));
    }

    public int getLightFromNeighborsFor(EnumSkyBlock type, BlockPos pos) {
        if (level == null || access instanceof net.minecraft.server.level.WorldGenRegion) return type.defaultLightValue;
        var p = modern(pos);
        int best = level.getBrightness(type.mcc$modern(), p);
        if (mcc$state(p).useShapeForLightOcclusion()) {
            for (Direction d : Direction.values()) best = Math.max(best, level.getBrightness(type.mcc$modern(), p.relative(d)));
        }
        return best;
    }

    /** The modern light engine keeps light; legacy writes to it are dropped. */
    public void setLightFor(EnumSkyBlock type, BlockPos pos, int lightValue) {
    }

    public void notifyLightSet(BlockPos pos) {
    }

    public boolean checkLight(BlockPos pos) {
        return true;
    }

    public boolean checkLightFor(EnumSkyBlock type, BlockPos pos) {
        return true;
    }

    public float getLightBrightness(BlockPos pos) {
        return level == null ? 1.0F : level.getLightLevelDependentMagicValue(modern(pos));
    }

    public int getBlockLightOpacity(BlockPos pos) {
        if (access == null) return 0;
        var p = modern(pos);
        return mcc$state(p).getLightBlock(access, p);
    }

    public int getSkylightSubtracted() {
        return level == null ? 0 : level.getSkyDarken();
    }

    public void setSkylightSubtracted(int value) {
    }

    public int calculateSkylightSubtracted(float partialTicks) {
        return getSkylightSubtracted();
    }

    public void calculateInitialSkylight() {
    }

    // ------------------------------------------------------------------ updates and redstone

    public void notifyBlockUpdate(BlockPos pos, IBlockState oldState, IBlockState newState, int flags) {
        Level live = mcc$liveLevel();
        if (live == null) return;
        if (live instanceof ServerLevel sl && !sl.getServer().isSameThread()) return;
        live.sendBlockUpdated(modern(pos), StatesBridge1122.toModern(oldState), StatesBridge1122.toModern(newState), flags);
    }

    public void markAndNotifyBlock(BlockPos pos, Chunk chunk, IBlockState oldState, IBlockState newState, int flags) {
        Level live = mcc$liveLevel();
        if (live == null) return;
        var p = modern(pos);
        live.markAndNotifyBlock(p, live.getChunkAt(p), StatesBridge1122.toModern(oldState), StatesBridge1122.toModern(newState), flags, 512);
    }

    public void markBlockRangeForRenderUpdate(BlockPos from, BlockPos to) {
        markBlockRangeForRenderUpdate(from.getX(), from.getY(), from.getZ(), to.getX(), to.getY(), to.getZ());
    }

    /** Client: the sections in the range are rebuilt. The server has nothing to redraw. */
    public void markBlockRangeForRenderUpdate(int x1, int y1, int z1, int x2, int y2, int z2) {
        if (!isRemote || level == null) return;
        org.kylepitcock.legacyconnector.v1_12_2.bridge.WorldClient1122.markRenderRange( x1, y1, z1, x2, y2, z2);
    }

    public void markBlocksDirtyVertical(int x, int z, int y1, int y2) {
        markBlockRangeForRenderUpdate(x, Math.min(y1, y2), z, x, Math.max(y1, y2), z);
    }

    public void markChunkDirty(BlockPos pos, TileEntity te) {
        Level live = mcc$liveLevel();
        if (live != null) live.blockEntityChanged(modern(pos));
    }

    public void notifyNeighborsOfStateChange(BlockPos pos, Block blockType, boolean updateObservers) {
        Level live = mcc$liveLevel();
        if (live != null) live.updateNeighborsAt(modern(pos), modernBlock(blockType));
    }

    public void notifyNeighborsRespectDebug(BlockPos pos, Block blockType, boolean updateObservers) {
        notifyNeighborsOfStateChange(pos, blockType, updateObservers);
    }

    public void notifyNeighborsOfStateExcept(BlockPos pos, Block blockType, EnumFacing skipSide) {
        Level live = mcc$liveLevel();
        if (live != null) live.updateNeighborsAtExceptFromFacing(modern(pos), modernBlock(blockType), skipSide == null ? null : skipSide.mcc$modern());
    }

    public void neighborChanged(BlockPos pos, Block blockIn, BlockPos fromPos) {
        Level live = mcc$liveLevel();
        if (live != null) live.neighborChanged(modern(pos), modernBlock(blockIn), modern(fromPos));
    }

    public void observedNeighborChanged(BlockPos pos, Block changedBlock, BlockPos changedBlockPos) {
        neighborChanged(pos, changedBlock, changedBlockPos);
    }

    public void updateObservingBlocksAt(BlockPos pos, Block blockType) {
        Level live = mcc$liveLevel();
        if (live == null) return;
        var p = modern(pos);
        for (Direction d : Direction.values()) {
            var other = p.relative(d);
            var state = live.getBlockState(other);
            if (state.getBlock() instanceof net.minecraft.world.level.block.ObserverBlock) live.neighborChanged(other, modernBlock(blockType), p);
        }
    }

    public void updateComparatorOutputLevel(BlockPos pos, Block blockIn) {
        Level live = mcc$liveLevel();
        if (live != null) live.updateNeighbourForOutputSignal(modern(pos), modernBlock(blockIn));
    }

    public void scheduleUpdate(BlockPos pos, Block blockIn, int delay) {
        if (access == null) return;
        access.scheduleTick(modern(pos), modernBlock(blockIn), delay);
    }

    public void scheduleBlockUpdate(BlockPos pos, Block blockIn, int delay, int priority) {
        if (access == null) return;
        access.scheduleTick(modern(pos), modernBlock(blockIn), delay, net.minecraft.world.ticks.TickPriority.byValue(Math.max(-3, Math.min(3, priority))));
    }

    public void updateBlockTick(BlockPos pos, Block blockIn, int delay, int priority) {
        scheduleBlockUpdate(pos, blockIn, delay, priority);
    }

    public boolean isUpdateScheduled(BlockPos pos, Block blockType) {
        return access != null && access.getBlockTicks().hasScheduledTick(modern(pos), modernBlock(blockType));
    }

    public boolean isBlockTickPending(BlockPos pos, Block blockType) {
        return access != null && access.getBlockTicks().willTickThisTick(modern(pos), modernBlock(blockType));
    }

    /** Runs the block's tick now (1.12's scheduledUpdatesAreImmediate path): the modern block's scheduled tick. */
    public void immediateBlockTick(BlockPos pos, IBlockState state, Random random) {
        if (!(access instanceof ServerLevel sl)) return;
        var p = modern(pos);
        BlockState s = StatesBridge1122.toModern(state);
        s.tick(sl, p, sl.getRandom());
    }

    public void addBlockEvent(BlockPos pos, Block blockIn, int eventID, int eventParam) {
        Level live = mcc$liveLevel();
        if (live != null) live.blockEvent(modern(pos), modernBlock(blockIn), eventID, eventParam);
    }

    public void sendBlockBreakProgress(int breakerId, BlockPos pos, int progress) {
        Level live = mcc$liveLevel();
        if (live != null) live.destroyBlockProgress(breakerId, modern(pos), progress);
    }

    public boolean isBlockPowered(BlockPos pos) {
        return access instanceof SignalGetter s && s.hasNeighborSignal(modern(pos));
    }

    public int isBlockIndirectlyGettingPowered(BlockPos pos) {
        return access instanceof SignalGetter s ? s.getBestNeighborSignal(modern(pos)) : 0;
    }

    public int getRedstonePowerFromNeighbors(BlockPos pos) {
        return isBlockIndirectlyGettingPowered(pos);
    }

    public int getRedstonePower(BlockPos pos, EnumFacing facing) {
        return access instanceof SignalGetter s ? s.getSignal(modern(pos), facing.mcc$modern()) : 0;
    }

    public boolean isSidePowered(BlockPos pos, EnumFacing side) {
        return getRedstonePower(pos, side) > 0;
    }

    // ------------------------------------------------------------------ tile entities

    @Override
    public TileEntity getTileEntity(BlockPos pos) {
        var p = modern(pos);
        if (access == null || outsideRegion(p) || unloaded(p)) return null;
        if (access instanceof ServerLevel sl && !sl.getServer().isSameThread()) {
            var holder = sl.getChunkSource().chunkMap.getVisibleChunkIfPresent(net.minecraft.world.level.ChunkPos.asLong(p.getX() >> 4, p.getZ() >> 4));
            var chunk = holder == null ? null : holder.getTickingChunk();
            var be = chunk == null ? null : chunk.getBlockEntity(p);
            return be instanceof org.kylepitcock.legacyconnector.v1_12_2.adapter.Legacy1122BlockEntity l ? l.legacy() : null;
        }
        var be = access.getBlockEntity(p);
        return be instanceof org.kylepitcock.legacyconnector.v1_12_2.adapter.Legacy1122BlockEntity l ? l.legacy() : null;
    }

    /** Puts a legacy tile entity at a position whose block hosts legacy tile entities (others have their own modern block entity). */
    public void setTileEntity(BlockPos pos, TileEntity tileEntity) {
        var p = modern(pos);
        if (access == null || tileEntity == null || outsideRegion(p)) return;
        var be = access.getBlockEntity(p);
        if (be instanceof org.kylepitcock.legacyconnector.v1_12_2.adapter.Legacy1122BlockEntity host) {
            tileEntity.setPos(pos);
            host.mcc$setLegacy(tileEntity);
        }
    }

    public boolean addTileEntity(TileEntity tile) {
        if (tile == null || tile.getPos() == null) return false;
        setTileEntity(tile.getPos(), tile);
        return true;
    }

    public void addTileEntities(java.util.Collection<TileEntity> tiles) {
        for (TileEntity te : tiles) addTileEntity(te);
    }

    public void removeTileEntity(BlockPos pos) {
        var p = modern(pos);
        if (access == null || outsideRegion(p)) return;
        Level live = mcc$liveLevel();
        if (live != null) live.removeBlockEntity(p);
        else access.getChunk(p).removeBlockEntity(p);
    }

    public void markTileEntityForRemoval(TileEntity tile) {
        if (tile != null && tile.getPos() != null) removeTileEntity(tile.getPos());
    }

    // ------------------------------------------------------------------ entities

    private static net.minecraft.world.entity.Entity modernOf(Entity e) {
        return e == null ? null : e.mcc$modern();
    }

    private static int spawnFailures;

    /**
     * Adds the entity to the world. A legacy entity is added through its modern counterpart; one the layer cannot host yet is not
     * added (logged, throttled).
     */
    public boolean spawnEntity(Entity entity) {
        if (entity == null || access == null) return false;
        var modern = modernOf(entity);
        if (modern == null) {
            modern = org.kylepitcock.legacyconnector.v1_12_2.bridge.WorldBridge1122.hostEntity(this, entity);
            if (modern == null) {
                if (spawnFailures++ < 5 || spawnFailures % 500 == 0) {
                    org.slf4j.LoggerFactory.getLogger("LegacyConnector/1.12.2").warn("Cannot spawn legacy entity {} yet ({} so far)", entity.getClass().getName(), spawnFailures);
                }
                return false;
            }
        }
        if (modern.isAddedToLevel() || modern.level() != level && modern.level() != null && modern.isAlive() && level != null && modern.level().getEntity(modern.getId()) == modern) {
            return false;
        }
        if (legacyconnector.v1_12_2.net.minecraft.entity.Entity.mcc$get$world(entity) == null) legacyconnector.v1_12_2.net.minecraft.entity.Entity.mcc$set$world(entity, this);
        return access.addFreshEntity(modern);
    }

    public boolean addWeatherEffect(Entity entity) {
        boolean added = spawnEntity(entity);
        if (added) weatherEffects.add(entity);
        return added;
    }

    public void joinEntityInSurroundings(Entity entity) {
        spawnEntity(entity);
    }

    public void loadEntities(java.util.Collection<Entity> entities) {
        for (Entity e : entities) spawnEntity(e);
    }

    public void unloadEntities(java.util.Collection<Entity> entities) {
        for (Entity e : entities) removeEntityDangerously(e);
    }

    public void removeEntity(Entity entity) {
        var modern = modernOf(entity);
        if (modern != null) modern.discard();
        if (entity != null) legacyconnector.v1_12_2.net.minecraft.entity.Entity.mcc$set$isDead(entity, true);
    }

    public void removeEntityDangerously(Entity entity) {
        var modern = modernOf(entity);
        if (modern != null) modern.discard();
        if (entity != null) legacyconnector.v1_12_2.net.minecraft.entity.Entity.mcc$set$isDead(entity, true);
    }

    public void onEntityAdded(Entity entity) {
    }

    public void onEntityRemoved(Entity entity) {
    }

    /** Modern entities tick themselves; legacy code asking for an update gets nothing extra. */
    public void updateEntity(Entity entity) {
    }

    public void updateEntityWithOptionalForce(Entity entity, boolean forceUpdate) {
    }

    public void updateEntities() {
    }

    public void setEntityState(Entity entity, byte state) {
        Level live = mcc$liveLevel();
        var modern = modernOf(entity);
        if (live != null && modern != null) live.broadcastEntityEvent(modern, state);
    }

    public Entity getEntityByID(int id) {
        Level live = mcc$liveLevel();
        if (live == null) return null;
        var e = live.getEntity(id);
        return e == null ? null : EntityBridge1122.wrap(e);
    }

    private List<net.minecraft.world.entity.Entity> modernEntities(net.minecraft.world.entity.Entity except, AxisAlignedBB bb) {
        Level live = mcc$liveLevel();
        if (live == null || bb == null) return List.of();
        if (live instanceof ServerLevel sl && !sl.getServer().isSameThread()) return List.of();
        return live.getEntities(except, bb.mcc$modern(), World::visibleToLegacy);
    }

    /** Item entities whose stack is empty (merged, removed next tick) are not reported: 1.12 never had empty item entities. */
    private static boolean visibleToLegacy(net.minecraft.world.entity.Entity e) {
        return !(e instanceof net.minecraft.world.entity.item.ItemEntity item) || !item.getItem().isEmpty();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public List getEntitiesWithinAABB(Class type, AxisAlignedBB bb) {
        return getEntitiesWithinAABB(type, bb, null);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public List getEntitiesWithinAABB(Class type, AxisAlignedBB bb, com.google.common.base.Predicate filter) {
        List out = new ArrayList<>();
        for (var e : modernEntities(null, bb)) {
            Entity legacy = EntityBridge1122.wrap(e);
            if (legacy != null && type.isInstance(legacy) && (filter == null || filter.apply(legacy))) out.add(legacy);
        }
        return out;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public List getEntitiesWithinAABBExcludingEntity(Entity except, AxisAlignedBB bb) {
        return getEntitiesInAABBexcluding(except, bb, null);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public List getEntitiesInAABBexcluding(Entity except, AxisAlignedBB bb, com.google.common.base.Predicate filter) {
        List out = new ArrayList<>();
        for (var e : modernEntities(modernOf(except), bb)) {
            Entity legacy = EntityBridge1122.wrap(e);
            if (legacy != null && legacy != except && (filter == null || filter.apply(legacy))) out.add(legacy);
        }
        return out;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public List getEntities(Class type, com.google.common.base.Predicate filter) {
        List out = new ArrayList<>();
        for (Entity e : mcc$get$loadedEntityList(this)) {
            if (e != null && type.isInstance(e) && (filter == null || filter.apply(e))) out.add(e);
        }
        return out;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public List getPlayers(Class type, com.google.common.base.Predicate filter) {
        List out = new ArrayList<>();
        for (EntityPlayer p : mcc$get$playerEntities(this)) {
            if (type.isInstance(p) && (filter == null || filter.apply(p))) out.add(p);
        }
        return out;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public Entity findNearestEntityWithinAABB(Class type, AxisAlignedBB bb, Entity closestTo) {
        Entity best = null;
        double bestDist = Double.MAX_VALUE;
        var center = modernOf(closestTo);
        for (Object o : getEntitiesWithinAABB(type, bb)) {
            Entity e = (Entity) o;
            if (e == closestTo) continue;
            var m = modernOf(e);
            double d = center == null || m == null ? 0.0D : m.distanceToSqr(center);
            if (d <= bestDist) {
                best = e;
                bestDist = d;
            }
        }
        return best;
    }

    public int countEntities(Class<?> type) {
        int n = 0;
        for (Entity e : mcc$get$loadedEntityList(this)) if (type.isInstance(e)) n++;
        return n;
    }

    public int countEntities(legacyconnector.v1_12_2.net.minecraft.entity.EnumCreatureType type, boolean forSpawnCount) {
        if (!(mcc$liveLevel() instanceof ServerLevel sl)) return 0;
        var category = switch (type) {
            case MONSTER -> net.minecraft.world.entity.MobCategory.MONSTER;
            case CREATURE -> net.minecraft.world.entity.MobCategory.CREATURE;
            case AMBIENT -> net.minecraft.world.entity.MobCategory.AMBIENT;
            case WATER_CREATURE -> net.minecraft.world.entity.MobCategory.WATER_CREATURE;
        };
        int n = 0;
        for (var e : sl.getAllEntities()) if (e.getType().getCategory() == category) n++;
        return n;
    }

    public boolean checkNoEntityCollision(AxisAlignedBB bb) {
        return checkNoEntityCollision(bb, null);
    }

    public boolean checkNoEntityCollision(AxisAlignedBB bb, Entity except) {
        Level live = mcc$liveLevel();
        if (live == null) return true;
        return live.isUnobstructed(modernOf(except), net.minecraft.world.phys.shapes.Shapes.create(bb.mcc$modern()));
    }

    private EntityPlayer legacyPlayer(net.minecraft.world.entity.player.Player p) {
        return p != null && EntityBridge1122.wrap(p) instanceof EntityPlayer legacy ? legacy : null;
    }

    public EntityPlayer getClosestPlayer(double x, double y, double z, double distance, boolean spectator) {
        if (access == null) return null;
        return legacyPlayer(access.getNearestPlayer(x, y, z, distance, spectator));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public EntityPlayer getClosestPlayer(double x, double y, double z, double distance, com.google.common.base.Predicate predicate) {
        EntityPlayer best = null;
        double bestDist = -1.0D;
        for (EntityPlayer p : mcc$get$playerEntities(this)) {
            if (predicate != null && !predicate.apply(p)) continue;
            var m = p.mcc$player();
            double d = m.distanceToSqr(x, y, z);
            if ((distance < 0.0D || d < distance * distance) && (bestDist == -1.0D || d < bestDist)) {
                bestDist = d;
                best = p;
            }
        }
        return best;
    }

    public EntityPlayer getClosestPlayerToEntity(Entity entity, double distance) {
        var m = modernOf(entity);
        return m == null ? null : getClosestPlayer(m.getX(), m.getY(), m.getZ(), distance, false);
    }

    public EntityPlayer getNearestPlayerNotCreative(Entity entity, double distance) {
        var m = modernOf(entity);
        return m == null ? null : getClosestPlayer(m.getX(), m.getY(), m.getZ(), distance, true);
    }

    public boolean isAnyPlayerWithinRangeAt(double x, double y, double z, double range) {
        return access != null && access.hasNearbyAlivePlayer(x, y, z, range);
    }

    public EntityPlayer getNearestAttackablePlayer(Entity entity, double maxXZDistance, double maxYDistance) {
        var m = modernOf(entity);
        return m == null ? null : getNearestAttackablePlayer(m.getX(), m.getY(), m.getZ(), maxXZDistance, maxYDistance, null, null);
    }

    public EntityPlayer getNearestAttackablePlayer(BlockPos pos, double maxXZDistance, double maxYDistance) {
        return getNearestAttackablePlayer(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, maxXZDistance, maxYDistance, null, null);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public EntityPlayer getNearestAttackablePlayer(double x, double y, double z, double maxXZDistance, double maxYDistance,
            com.google.common.base.Function playerToDouble, com.google.common.base.Predicate predicate) {
        EntityPlayer best = null;
        double bestDist = -1.0D;
        for (EntityPlayer p : mcc$get$playerEntities(this)) {
            var m = p.mcc$player();
            if (m.getAbilities().invulnerable || !m.isAlive() || m.isSpectator()) continue;
            if (predicate != null && !predicate.apply(p)) continue;
            double d = m.distanceToSqr(x, m.getY(), z);
            double range = maxXZDistance;
            if (m.isShiftKeyDown()) range *= 0.8D;
            if (playerToDouble != null) {
                Object factor = playerToDouble.apply(p);
                if (factor instanceof Number n) range *= n.doubleValue();
            }
            if ((maxYDistance < 0.0D || Math.abs(m.getY() - y) < maxYDistance * maxYDistance) && (maxXZDistance < 0.0D || d < range * range)
                    && (bestDist == -1.0D || d < bestDist)) {
                bestDist = d;
                best = p;
            }
        }
        return best;
    }

    public EntityPlayer getPlayerEntityByName(String name) {
        if (level == null) return null;
        for (var p : level.players()) {
            if (p.getGameProfile().getName().equals(name)) return legacyPlayer(p);
        }
        return null;
    }

    public EntityPlayer getPlayerEntityByUUID(java.util.UUID uuid) {
        return level == null || uuid == null ? null : legacyPlayer(level.getPlayerByUUID(uuid));
    }

    public boolean isInsideWorldBorder(Entity entity) {
        var m = modernOf(entity);
        return level == null || m == null || level.getWorldBorder().isWithinBounds(m.getBoundingBox());
    }

    // ------------------------------------------------------------------ sounds and effects

    /** The modern sound of a legacy one: a vanilla event, or a mod sound by its name (the client resolves it from sounds.json). */
    private static net.minecraft.sounds.SoundEvent modernSound(SoundEvent sound) {
        if (sound == null) return null;
        if (sound.mcc$modern != null) return sound.mcc$modern;
        var name = sound.getSoundName() != null ? sound.getSoundName() : sound.getRegistryName();
        if (name == null) return null;
        sound.mcc$modern = net.minecraft.sounds.SoundEvent.createVariableRangeEvent(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(
                name.getNamespace(), name.getPath()));
        return sound.mcc$modern;
    }

    private static net.minecraft.sounds.SoundSource source(SoundCategory category) {
        if (category == null) return net.minecraft.sounds.SoundSource.MASTER;
        return switch (category) {
            case MASTER -> net.minecraft.sounds.SoundSource.MASTER;
            case MUSIC -> net.minecraft.sounds.SoundSource.MUSIC;
            case RECORDS -> net.minecraft.sounds.SoundSource.RECORDS;
            case WEATHER -> net.minecraft.sounds.SoundSource.WEATHER;
            case BLOCKS -> net.minecraft.sounds.SoundSource.BLOCKS;
            case HOSTILE -> net.minecraft.sounds.SoundSource.HOSTILE;
            case NEUTRAL -> net.minecraft.sounds.SoundSource.NEUTRAL;
            case PLAYERS -> net.minecraft.sounds.SoundSource.PLAYERS;
            case AMBIENT -> net.minecraft.sounds.SoundSource.AMBIENT;
            case VOICE -> net.minecraft.sounds.SoundSource.VOICE;
        };
    }

    public void playSound(EntityPlayer player, BlockPos pos, SoundEvent sound, SoundCategory category, float volume, float pitch) {
        playSound(player, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, sound, category, volume, pitch);
    }

    public void playSound(EntityPlayer player, double x, double y, double z, SoundEvent sound, SoundCategory category, float volume, float pitch) {
        Level live = mcc$liveLevel();
        var s = modernSound(sound);
        if (live == null || s == null) return;
        net.minecraft.world.entity.player.Player except = player == null ? null : player.mcc$player();
        live.playSound(except, x, y, z, s, source(category), volume, pitch);
    }

    /** Client only: plays the sound locally. */
    public void playSound(double x, double y, double z, SoundEvent sound, SoundCategory category, float volume, float pitch, boolean distanceDelay) {
        Level live = mcc$liveLevel();
        var s = modernSound(sound);
        if (live == null || s == null) return;
        live.playLocalSound(x, y, z, s, source(category), volume, pitch, distanceDelay);
    }

    public void playRecord(BlockPos pos, SoundEvent sound) {
    }

    /** Level event ids are the same as 1.12's; the block-break event's data is a block state id, translated. */
    private static int eventData(int type, int data) {
        if (type == 2001) {
            try {
                IBlockState state = Block.getStateById(data);
                return net.minecraft.world.level.block.Block.getId(StatesBridge1122.toModern(state));
            } catch (RuntimeException | LinkageError e) {
                return 0;
            }
        }
        return data;
    }

    public void playEvent(int type, BlockPos pos, int data) {
        playEvent(null, type, pos, data);
    }

    public void playEvent(EntityPlayer player, int type, BlockPos pos, int data) {
        Level live = mcc$liveLevel();
        if (live == null) return;
        live.levelEvent(player == null ? null : player.mcc$player(), type, modern(pos), eventData(type, data));
    }

    public void playBroadcastSound(int id, BlockPos pos, int data) {
        Level live = mcc$liveLevel();
        if (live != null) live.globalLevelEvent(id, modern(pos), data);
    }

    public Explosion createExplosion(Entity entity, double x, double y, double z, float strength, boolean damagesTerrain) {
        return newExplosion(entity, x, y, z, strength, false, damagesTerrain);
    }

    public Explosion newExplosion(Entity entity, double x, double y, double z, float strength, boolean causesFire, boolean damagesTerrain) {
        Level live = mcc$liveLevel();
        if (live == null) return new Explosion(this, entity, x, y, z, strength, causesFire, damagesTerrain);
        var modern = live.explode(modernOf(entity), x, y, z, strength, causesFire,
                damagesTerrain ? Level.ExplosionInteraction.TNT : Level.ExplosionInteraction.NONE);
        return Explosion.mcc$of(this, entity, modern);
    }

    public void makeFireworks(double x, double y, double z, double motionX, double motionY, double motionZ,
            legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound compound) {
    }

    // ------------------------------------------------------------------ time and weather

    public long getSeed() {
        if (access instanceof WorldGenLevel g) return g.getSeed();
        return level instanceof ServerLevel sl ? sl.getSeed() : mcc$get$worldInfo(this).getSeed();
    }

    public long getTotalWorldTime() {
        return level == null ? 0L : level.getGameTime();
    }

    public long getWorldTime() {
        return level == null ? 0L : level.getDayTime();
    }

    public void setWorldTime(long time) {
        if (level instanceof ServerLevel sl) sl.setDayTime(time);
    }

    public void setTotalWorldTime(long time) {
        mcc$get$worldInfo(this).setWorldTotalTime(time);
    }

    public boolean isDaytime() {
        return level != null && level.isDay();
    }

    public boolean isDaytimeNow() {
        return isDaytime();
    }

    public float getCelestialAngle(float partialTicks) {
        return level == null ? 0.0F : level.getTimeOfDay(partialTicks);
    }

    public float getCelestialAngleRadians(float partialTicks) {
        return getCelestialAngle(partialTicks) * (float) (Math.PI * 2D);
    }

    public int getMoonPhase() {
        return level == null ? 0 : level.getMoonPhase();
    }

    public float getCurrentMoonPhaseFactor() {
        return mcc$get$provider(this).getCurrentMoonPhaseFactor();
    }

    public float getCurrentMoonPhaseFactorBody() {
        return WorldProvider.MOON_PHASE_FACTORS[getMoonPhase()];
    }

    public float getSunBrightnessFactor(float partialTicks) {
        float f = getCelestialAngle(partialTicks);
        float f1 = 1.0F - ((float) Math.cos(f * (Math.PI * 2D)) * 2.0F + 0.5F);
        f1 = Math.max(0.0F, Math.min(1.0F, f1));
        f1 = 1.0F - f1;
        f1 = (float) (f1 * (1.0D - getRainStrength(partialTicks) * 5.0F / 16.0D));
        f1 = (float) (f1 * (1.0D - getThunderStrength(partialTicks) * 5.0F / 16.0D));
        return f1;
    }

    public float getSunBrightness(float partialTicks) {
        return mcc$get$provider(this).getSunBrightness(partialTicks);
    }

    public float getSunBrightnessBody(float partialTicks) {
        return getSunBrightnessFactor(partialTicks) * 0.8F + 0.2F;
    }

    public float getStarBrightness(float partialTicks) {
        return mcc$get$provider(this).getStarBrightness(partialTicks);
    }

    public float getStarBrightnessBody(float partialTicks) {
        float f = getCelestialAngle(partialTicks);
        float f1 = 1.0F - ((float) Math.cos(f * (Math.PI * 2D)) * 2.0F + 0.25F);
        f1 = Math.max(0.0F, Math.min(1.0F, f1));
        return f1 * f1 * 0.5F;
    }

    public Vec3d getSkyColor(Entity camera, float partialTicks) {
        return mcc$get$provider(this).getSkyColor(camera, partialTicks);
    }

    public Vec3d getSkyColorBody(Entity camera, float partialTicks) {
        if (level != null && level.isClientSide()) {
            var m = modernOf(camera);
            var at = m != null ? m.position() : net.minecraft.world.phys.Vec3.ZERO;
            return Vec3d.mcc$of(org.kylepitcock.legacyconnector.v1_12_2.bridge.WorldClient1122.skyColor(level, at, partialTicks));
        }
        return new Vec3d(0.47D, 0.65D, 1.0D);
    }

    public Vec3d getCloudColour(float partialTicks) {
        return mcc$get$provider(this).getCloudColor(partialTicks);
    }

    public Vec3d getCloudColorBody(float partialTicks) {
        if (level != null && level.isClientSide()) return Vec3d.mcc$of(org.kylepitcock.legacyconnector.v1_12_2.bridge.WorldClient1122.cloudColor(level, partialTicks));
        return new Vec3d(1.0D, 1.0D, 1.0D);
    }

    public Vec3d getFogColor(float partialTicks) {
        return mcc$get$provider(this).getFogColor(getCelestialAngle(partialTicks), partialTicks);
    }

    public double getHorizon() {
        return mcc$get$provider(this).getHorizon();
    }

    public boolean isRaining() {
        return level != null && level.isRaining();
    }

    public boolean isThundering() {
        return level != null && level.isThundering();
    }

    public boolean isRainingAt(BlockPos pos) {
        return level != null && !(access instanceof net.minecraft.server.level.WorldGenRegion) && level.isRainingAt(modern(pos));
    }

    public float getRainStrength(float delta) {
        return level == null ? 0.0F : level.getRainLevel(delta);
    }

    public void setRainStrength(float strength) {
        if (level != null) level.setRainLevel(strength);
    }

    public float getThunderStrength(float delta) {
        return level == null ? 0.0F : level.getThunderLevel(delta);
    }

    public void setThunderStrength(float strength) {
        if (level != null) level.setThunderLevel(strength);
    }

    public boolean isBlockinHighHumidity(BlockPos pos) {
        return mcc$get$provider(this).isBlockHighHumidity(pos);
    }

    public boolean canBlockFreezeWater(BlockPos pos) {
        return canBlockFreeze(pos, false);
    }

    public boolean canBlockFreezeNoWater(BlockPos pos) {
        return canBlockFreeze(pos, true);
    }

    public boolean canBlockFreeze(BlockPos pos, boolean noWaterAdj) {
        return mcc$get$provider(this).canBlockFreeze(pos, noWaterAdj);
    }

    public boolean canBlockFreezeBody(BlockPos pos, boolean noWaterAdj) {
        if (level == null) return false;
        var p = modern(pos);
        return level.getBiome(p).value().shouldFreeze(level, p, !noWaterAdj);
    }

    public boolean canSnowAt(BlockPos pos, boolean checkLight) {
        return mcc$get$provider(this).canSnowAt(pos, checkLight);
    }

    public boolean canSnowAtBody(BlockPos pos, boolean checkLight) {
        if (level == null) return false;
        var p = modern(pos);
        return level.getBiome(p).value().shouldSnow(level, p);
    }

    public void updateWeatherBody() {
    }

    public void calculateInitialWeatherBody() {
    }

    public int getLastLightningBolt() {
        return lastLightningBolt;
    }

    public void setLastLightningBolt(int ticks) {
        lastLightningBolt = ticks;
    }

    public java.util.Calendar getCurrentDate() {
        java.util.Calendar c = java.util.Calendar.getInstance();
        if (getTotalWorldTime() % 600L == 0L) c.setTimeInMillis(System.currentTimeMillis());
        return c;
    }

    // ------------------------------------------------------------------ world properties

    public int getDifficultyId() {
        return level == null ? 2 : level.getDifficulty().getId();
    }

    public EnumDifficulty getDifficulty() {
        return level == null ? EnumDifficulty.NORMAL : EnumDifficulty.mcc$of(level.getDifficulty());
    }

    public DifficultyInstance getDifficultyForLocation(BlockPos pos) {
        if (access == null) return new DifficultyInstance(EnumDifficulty.NORMAL, 0L, 0L, 0.0F);
        return DifficultyInstance.mcc$of(access.getCurrentDifficultyAt(modern(pos)));
    }

    public GameRules getGameRules() {
        return level == null ? new GameRules() : GameRules.mcc$of(level.getGameRules(), level.getServer());
    }

    public legacyconnector.v1_12_2.net.minecraft.world.storage.WorldInfo getWorldInfo() {
        return mcc$get$worldInfo(this);
    }

    public WorldProvider getProvider() {
        return mcc$get$provider(this);
    }

    public String getProviderName() {
        return getChunkProvider().makeString();
    }

    public legacyconnector.v1_12_2.net.minecraft.server.MinecraftServer getMinecraftServer() {
        return level != null && level.getServer() != null ? legacyconnector.v1_12_2.net.minecraft.server.MinecraftServer.mcc$instance() : null;
    }

    public legacyconnector.v1_12_2.net.minecraft.world.storage.ISaveHandler getSaveHandler() {
        return mcc$get$saveHandler(this);
    }

    public legacyconnector.v1_12_2.net.minecraft.world.border.WorldBorder getWorldBorder() {
        return legacyconnector.v1_12_2.net.minecraft.world.border.WorldBorder.mcc$of(level == null ? null : level.getWorldBorder());
    }

    public int getSeaLevel() {
        return seaLevel;
    }

    public void setSeaLevel(int seaLevel) {
        this.seaLevel = seaLevel;
    }

    public int getHeight() {
        return access == null ? 256 : access.getMaxBuildHeight();
    }

    public int getActualHeight() {
        return mcc$get$provider(this).isNether() ? 128 : getHeight();
    }

    public BlockPos getSpawnPoint() {
        if (level == null) return new BlockPos(0, 64, 0);
        return BlockPos.mcc$of(level.getSharedSpawnPos());
    }

    public void setSpawnPoint(BlockPos pos) {
        if (level instanceof ServerLevel sl) sl.setDefaultSpawnPos(modern(pos), 0.0F);
    }

    public boolean isSpawnChunk(int x, int z) {
        BlockPos spawn = getSpawnPoint();
        int dx = x * 16 + 8 - spawn.getX();
        int dz = z * 16 + 8 - spawn.getZ();
        return dx >= -128 && dx <= 128 && dz >= -128 && dz <= 128;
    }

    public java.util.Random setRandomSeed(int seedX, int seedY, int seedZ) {
        long seed = seedX * 341873128712L + seedY * 132897987541L + getSeed() + seedZ;
        rand.setSeed(seed);
        return rand;
    }

    public legacyconnector.v1_12_2.net.minecraft.world.biome.BiomeProvider getBiomeProvider() {
        var provided = mcc$get$provider(this).getBiomeProvider();
        return provided != null ? provided : legacyconnector.v1_12_2.net.minecraft.world.biome.BiomeProvider.mcc$of(this);
    }

    // ------------------------------------------------------------------ chunks and storage

    public Chunk getChunk(BlockPos pos) {
        return getChunk(pos.getX() >> 4, pos.getZ() >> 4);
    }

    /** A view of the chunk, loaded if needed on the server thread; elsewhere only an available chunk (an empty one otherwise). */
    public Chunk getChunk(int chunkX, int chunkZ) {
        return getChunkProvider().provideChunk(chunkX, chunkZ);
    }

    public legacyconnector.v1_12_2.net.minecraft.world.chunk.IChunkProvider getChunkProvider() {
        return mcc$serverChunkProvider();
    }

    /** One provider per world, as in 1.12 (mods keep it and read its fields). */
    public legacyconnector.v1_12_2.net.minecraft.world.gen.ChunkProviderServer mcc$serverChunkProvider() {
        var provider = mcc$chunkProvider;
        if (provider == null) mcc$chunkProvider = provider = new legacyconnector.v1_12_2.net.minecraft.world.gen.ChunkProviderServer(this);
        return provider;
    }

    public boolean isChunkGeneratedAt(int x, int z) {
        return mcc$serverChunkProvider().isChunkGeneratedAt(x, z);
    }

    public legacyconnector.v1_12_2.net.minecraft.world.storage.MapStorage getMapStorage() {
        return legacyconnector.v1_12_2.net.minecraft.world.storage.MapStorage.mcc$of(level instanceof ServerLevel s ? s.getServer().overworld() : null);
    }

    public legacyconnector.v1_12_2.net.minecraft.world.storage.MapStorage getPerWorldStorage() {
        return legacyconnector.v1_12_2.net.minecraft.world.storage.MapStorage.mcc$of(level instanceof ServerLevel s ? s : null);
    }

    public void setData(String dataID, legacyconnector.v1_12_2.net.minecraft.world.storage.WorldSavedData worldSavedDataIn) {
        getMapStorage().setData(dataID, worldSavedDataIn);
    }

    public legacyconnector.v1_12_2.net.minecraft.world.storage.WorldSavedData loadData(Class<? extends legacyconnector.v1_12_2.net.minecraft.world.storage.WorldSavedData> clazz,
            String dataID) {
        return getMapStorage().getOrLoadData(clazz, dataID);
    }

    public int getUniqueDataId(String key) {
        return getMapStorage().getUniqueDataId(key);
    }

    public com.google.common.collect.ImmutableSetMultimap<legacyconnector.v1_12_2.net.minecraft.util.math.ChunkPos, legacyconnector.v1_12_2.net.minecraftforge.common.ForgeChunkManager.Ticket> getPersistentChunks() {
        return legacyconnector.v1_12_2.net.minecraftforge.common.ForgeChunkManager.getPersistentChunksFor(this);
    }

    public java.util.Iterator<Chunk> getPersistentChunkIterable(java.util.Iterator<Chunk> chunkIterator) {
        return chunkIterator;
    }

    /** The nearest structure of a 1.12 name (Stronghold, Village, Mansion...), found by the modern game; null when none. */
    public BlockPos findNearestStructure(String structureName, BlockPos position, boolean findUnexplored) {
        if (!(mcc$liveLevel() instanceof ServerLevel sl) || !sl.getServer().isSameThread()) return null;
        var tag = org.kylepitcock.legacyconnector.v1_12_2.bridge.WorldBridge1122.structureTag(structureName);
        if (tag == null) return null;
        var found = sl.findNearestMapStructure(tag, modern(position), 100, findUnexplored);
        return found == null ? null : BlockPos.mcc$of(found);
    }

    public boolean hasCapability(legacyconnector.v1_12_2.net.minecraftforge.common.capabilities.Capability<?> capability, EnumFacing facing) {
        return false;
    }

    public <T> T getCapability(legacyconnector.v1_12_2.net.minecraftforge.common.capabilities.Capability<T> capability, EnumFacing facing) {
        return null;
    }

    // ------------------------------------------------------------------ the server loop (run by the modern game)

    public World init() {
        return this;
    }

    public void initialize(WorldSettings settings) {
    }

    public void tick() {
    }

    public boolean tickUpdates(boolean runAllPending) {
        return false;
    }

    public void checkSessionLock() {
    }

    public void sendQuittingDisconnectingPacket() {
    }

    public void setInitialSpawnLocation() {
    }

    public void setAllowedSpawnTypes(boolean hostile, boolean peaceful) {
    }

    public void updateAllPlayersSleepingFlag() {
    }

    public String getDebugLoadedEntities() {
        return "All: " + mcc$get$loadedEntityList(this).size();
    }

    public List<Entity> getLoadedEntityList() {
        return mcc$get$loadedEntityList(this);
    }

    public legacyconnector.v1_12_2.net.minecraft.crash.CrashReportCategory addWorldInfoToCrashReport(legacyconnector.v1_12_2.net.minecraft.crash.CrashReport report) {
        return report.makeCategoryDepth("Affected level", 1);
    }


    /** The world's scoreboard (area F; the server's on a server, the client's on a client). */
    public legacyconnector.v1_12_2.net.minecraft.scoreboard.Scoreboard getScoreboard() {
        var level = mcc$level();
        return level == null ? null : legacyconnector.v1_12_2.net.minecraft.scoreboard.Scoreboard.mcc$of(level.getScoreboard());
    }
}
