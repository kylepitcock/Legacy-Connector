package legacyconnector.v1_7_10.net.minecraft.world;

import java.util.ArrayList;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.BlockAccessView;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.WeakHashMap;
import legacyconnector.v1_7_10.net.minecraft.block.Block;
import legacyconnector.v1_7_10.net.minecraft.entity.Entity;
import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;
import legacyconnector.v1_7_10.net.minecraft.tileentity.TileEntity;
import legacyconnector.v1_7_10.net.minecraft.util.AxisAlignedBB;
import legacyconnector.v1_7_10.net.minecraft.util.ChunkCoordinates;
import legacyconnector.v1_7_10.net.minecraftforge.common.util.ForgeDirection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.SignalGetter;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.BlockBridge;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.EntityBridge;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.SoundBridge;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.TileEntityBridge;

/**
 * The 1.7.10 World API as a view over a modern {@link LevelAccessor}: a live {@link Level} during gameplay, or a
 * {@code WorldGenRegion} while legacy world generators run. Coordinates are absolute in both versions, so legacy
 * code keeps working even though worlds now span y=-64..319.
 */
public class World implements IBlockAccess {
    private static final Map<LevelAccessor, World> VIEWS = Collections.synchronizedMap(new WeakHashMap<>());

    public final boolean isRemote;
    /** Phantom: redirected to {@link #mcc$get$difficultySetting}. */
    @Deprecated
    public EnumDifficulty difficultySetting;
    public Random rand = new Random();
    public final legacyconnector.v1_7_10.net.minecraft.profiler.Profiler theProfiler = legacyconnector.v1_7_10.net.minecraft.profiler.Profiler.INSTANCE;
    /** Phantom: redirected to {@link #mcc$get$provider} (created on first use: mod providers need a usable world). */
    @Deprecated
    public WorldProvider provider;
    private WorldProvider mcc$provider;
    public boolean scheduledUpdatesAreImmediate;
    /** Forge: how far an entity's box may reach past its chunk; collision and selection searches widen by it. */
    public static double MAX_ENTITY_RADIUS = 2.0D;
    /** The list collision searches fill (mods' hooks reuse it, as vanilla's own search did). */
    public java.util.ArrayList collidingBoundingBoxes = new java.util.ArrayList();
    /** Phantom: redirected to {@link #mcc$get$playerEntities}. */
    @Deprecated
    public List playerEntities;
    /** Phantom: redirected to {@link #mcc$get$loadedEntityList}. */
    @Deprecated
    public List loadedEntityList;
    /** Phantom: redirected to {@link #mcc$get$loadedTileEntityList}. */
    @Deprecated
    public List loadedTileEntityList;

    /** Phantom: redirected to {@link #mcc$get$mapStorage}. Global (overworld) saved data. */
    @Deprecated
    public legacyconnector.v1_7_10.net.minecraft.world.storage.MapStorage mapStorage;
    /** Phantom: redirected to {@link #mcc$get$perWorldStorage}. Saved data of this dimension. */
    @Deprecated
    public legacyconnector.v1_7_10.net.minecraft.world.storage.MapStorage perWorldStorage;

    private static final Map<Object, legacyconnector.v1_7_10.net.minecraft.world.storage.MapStorage> STORAGES =
            Collections.synchronizedMap(new WeakHashMap<>());
    private static final legacyconnector.v1_7_10.net.minecraft.world.storage.MapStorage CLIENT_STORAGE =
            new legacyconnector.v1_7_10.net.minecraft.world.storage.MapStorage((net.minecraft.world.level.storage.DimensionDataStorage) null);

    private static legacyconnector.v1_7_10.net.minecraft.world.storage.MapStorage storage(ServerLevel level) {
        if (level == null) return CLIENT_STORAGE;
        var data = level.getDataStorage();
        return STORAGES.computeIfAbsent(data, d -> new legacyconnector.v1_7_10.net.minecraft.world.storage.MapStorage(data));
    }

    public static legacyconnector.v1_7_10.net.minecraft.world.storage.MapStorage mcc$get$mapStorage(World w) {
        return storage(w.mcc$fullLevel() instanceof ServerLevel s ? s.getServer().overworld() : null);
    }

    public static legacyconnector.v1_7_10.net.minecraft.world.storage.MapStorage mcc$get$perWorldStorage(World w) {
        return storage(w.mcc$fullLevel() instanceof ServerLevel s ? s : null);
    }

    public WorldSavedData loadItemData(Class<?> type, String name) {
        return mcc$get$mapStorage(this).loadData(type, name);
    }

    public void setItemData(String name, WorldSavedData data) {
        mcc$get$mapStorage(this).setData(name, data);
    }

    public int getUniqueDataId(String key) {
        return mcc$get$mapStorage(this).getUniqueDataId(key);
    }

    private final LevelAccessor mcc$level;

    protected World(LevelAccessor level) {
        this.mcc$level = level;
        this.isRemote = level != null && level.isClientSide();
    }

    /** A world a mod builds itself (a fake world for rendering or simulation): it has no level behind it, so every query sees empty space. */
    public World(legacyconnector.v1_7_10.net.minecraft.world.storage.ISaveHandler saveHandler, String name, WorldSettings settings, WorldProvider provider,
            legacyconnector.v1_7_10.net.minecraft.profiler.Profiler profiler) {
        this((LevelAccessor) null);
    }

    protected World(legacyconnector.v1_7_10.net.minecraft.world.storage.ISaveHandler saveHandler, String name, WorldProvider provider, WorldSettings settings,
            legacyconnector.v1_7_10.net.minecraft.profiler.Profiler profiler) {
        this((LevelAccessor) null);
    }

    /**
     * While set on a thread, this world's methods act on the given generation region instead of the full level
     * (legacy chunk providers keep the full world and run populate/decorate from world generation threads, where
     * touching the live level deadlocks).
     */
    private static final ThreadLocal<LevelAccessor> REGION = new ThreadLocal<>();

    /** Routes all legacy worlds of the region's level to the region on this thread; returns the previous value. */
    public static LevelAccessor mcc$enterRegion(LevelAccessor region) {
        LevelAccessor previous = REGION.get();
        REGION.set(region);
        return previous;
    }

    public static void mcc$leaveRegion(LevelAccessor previous) {
        if (previous == null) REGION.remove();
        else REGION.set(previous);
    }

    private LevelAccessor lvl() {
        LevelAccessor region = REGION.get();
        if (region != null && region != mcc$level && mcc$level instanceof Level full && mcc$fullLevel(region) == full) return region;
        return mcc$level;
    }

    /** Phantom: redirected to {@link #mcc$get$villageCollectionObj}. */
    @Deprecated
    public legacyconnector.v1_7_10.net.minecraft.village.VillageCollection villageCollectionObj;

    private legacyconnector.v1_7_10.net.minecraft.village.VillageCollection mcc$villages;

    public static legacyconnector.v1_7_10.net.minecraft.village.VillageCollection mcc$get$villageCollectionObj(World w) {
        if (w.mcc$villages == null) w.mcc$villages = new legacyconnector.v1_7_10.net.minecraft.village.VillageCollection(w);
        return w.mcc$villages;
    }

    public legacyconnector.v1_7_10.net.minecraft.village.VillageCollection getVillageCollection() {
        return mcc$get$villageCollectionObj(this);
    }

    public static WorldProvider mcc$get$provider(World w) {
        WorldProvider p = w.mcc$provider;
        if (p != null) return p;
        synchronized (w) {
            if (w.mcc$provider != null) return w.mcc$provider;
            Level full = w.mcc$fullLevel();
            WorldProvider created = full == null ? new WorldProvider(Level.OVERWORLD, true, false)
                    : org.kylepitcock.legacyconnector.v1_7_10.dimension.LegacyDimensions.newProvider(full);
            w.mcc$provider = created;
            if (full != null && created.worldObj == null) created.registerWorld(w);
            return created;
        }
    }

    /**
     * The view of a world generation region, per thread. Regions live for one generation step; in {@link #VIEWS} (weak keys)
     * they would never be released, because the view holds its region strongly: every region a legacy generator touched,
     * with all its proto chunks, stayed in memory (gigabytes after a few minutes of exploring).
     */
    private static final ThreadLocal<World> REGION_VIEW = new ThreadLocal<>();

    public static World mcc$wrap(LevelAccessor level) {
        if (level == null) return null;
        if (level instanceof net.minecraft.world.level.WorldGenLevel && !(level instanceof Level)) {
            World cached = REGION_VIEW.get();
            if (cached != null && cached.mcc$level == level) return cached;
            World view = new WorldServer(level);
            REGION_VIEW.set(view);
            return view;
        }
        return VIEWS.computeIfAbsent(level, l -> l instanceof net.minecraft.server.level.ServerLevel ? new WorldServer(l)
                : l.isClientSide() ? new legacyconnector.v1_7_10.net.minecraft.client.multiplayer.WorldClient(l) : new World(l));
    }

    public LevelAccessor mcc$level() {
        return lvl();
    }

    /** The level's scoreboard (the server's, or the copy the server keeps a client in sync with). */
    public legacyconnector.v1_7_10.net.minecraft.scoreboard.Scoreboard getScoreboard() {
        Level full = mcc$fullLevel();
        return full == null ? null : legacyconnector.v1_7_10.net.minecraft.scoreboard.Scoreboard.mcc$wrap(full.getScoreboard());
    }

    /** The full level, or null for a detached world-gen region without one. */
    public Level mcc$fullLevel() {
        return mcc$fullLevel(lvl());
    }

    /**
     * The level for block updates, neighbour notifications, sounds and entities: null while this world is a
     * generation region (calls would reach the live level from a world-gen thread and deadlock waiting for chunks).
     */
    public Level mcc$liveLevel() {
        return lvl() instanceof Level l ? l : null;
    }

    /**
     * Views of one level are the same world, as 1.7.10 had a single World object per dimension: generation hands out a
     * region view per chunk, and mods caching per world in a HashMap (CustomOreGen's configs) reloaded their data for every
     * chunk and kept every view.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof World other)) return false;
        Level mine = mcc$fullLevel();
        return mine != null && mine == other.mcc$fullLevel();
    }

    @Override
    public int hashCode() {
        Level level = mcc$fullLevel();
        return level != null ? System.identityHashCode(level) : System.identityHashCode(this);
    }

    private static Level mcc$fullLevel(LevelAccessor level) {
        if (level instanceof Level l) return l;
        if (level instanceof WorldGenLevel g) return g.getLevel();
        return null;
    }

    protected static BlockPos pos(int x, int y, int z) {
        return DEEP_PASSES.get() == 0 ? new BlockPos(x, y, z) : new BlockPos(x, y + deepShift(), z);
    }

    /**
     * Deep pass: 1.7.10 worlds ended at y=0 with bedrock, modern ones at -64. While a deep pass runs on a thread, legacy
     * generators see the world shifted up by {@code -shift} (legacy y=0 is modern -64), deepslate reads as stone, and
     * only writes below y=0 are accepted: ores, bedrock ores and underground structures land in the deep layer too.
     */
    private static final ThreadLocal<int[]> DEEP_SHIFT = new ThreadLocal<>();
    private static final java.util.concurrent.atomic.AtomicInteger DEEP_PASSES = new java.util.concurrent.atomic.AtomicInteger();

    public static void mcc$enterDeepPass(int shift) {
        DEEP_PASSES.incrementAndGet();
        DEEP_SHIFT.set(new int[] {shift});
    }

    public static void mcc$leaveDeepPass() {
        DEEP_SHIFT.remove();
        DEEP_PASSES.decrementAndGet();
    }

    private static int deepShift() {
        int[] s = DEEP_SHIFT.get();
        return s == null ? 0 : s[0];
    }

    private static boolean inDeepPass() {
        return DEEP_PASSES.get() != 0 && DEEP_SHIFT.get() != null;
    }

    /** A modern height (surface scans) as the legacy code of this thread sees it. */
    private static int legacyY(int modernY) {
        return DEEP_PASSES.get() == 0 ? modernY : modernY - deepShift();
    }

    // ------------------------------------------------------------------ virtual fields

    public static EnumDifficulty mcc$get$difficultySetting(World w) {
        return EnumDifficulty.mcc$of(w.lvl().getDifficulty());
    }

    public static void mcc$set$difficultySetting(World w, EnumDifficulty d) {
        // the difficulty is a server setting; legacy code that sets it directly changes nothing
    }

    public static List mcc$get$playerEntities(World w) {
        List<EntityPlayer> out = new ArrayList<>();
        for (var p : w.lvl().players()) out.add((EntityPlayer) EntityBridge.wrap(p));
        return out;
    }

    /** The entity list of this server tick (see {@link #mcc$get$loadedEntityList}). */
    private List<Entity> mcc$entityList;
    private int mcc$entityListTick = -1;

    /**
     * The loaded entities, built once per server tick. Mods loop over it by index and read the field on every step
     * ({@code for (i < world.loadedEntityList.size()) world.loadedEntityList.get(i)}: Portal Gun every tick), so building it on each
     * read cost entities squared per tick. Entities legacy code spawns or removes through this world are added or dropped at once.
     */
    public static List mcc$get$loadedEntityList(World w) {
        Level level = w.mcc$fullLevel();
        if (!(level instanceof ServerLevel s)) return new ArrayList<>();
        int tick = s.getServer().getTickCount();
        if (w.mcc$entityList != null && w.mcc$entityListTick == tick) return w.mcc$entityList;
        List<Entity> out = new ArrayList<>();
        for (var e : s.getAllEntities()) out.add(EntityBridge.wrap(e));
        w.mcc$entityList = out;
        w.mcc$entityListTick = tick;
        return out;
    }

    public static List mcc$get$loadedTileEntityList(World w) {
        return new ArrayList<>();
    }

    // ------------------------------------------------------------------ blocks

    /**
     * A server level that has not loaded the chunk: legacy code read such blocks as air, while the modern level would load the
     * chunk and wait for it, which deadlocks when a tile entity reads its neighbours as its own chunk unloads (world save and exit hang).
     * Likewise a chunk outside the generation region a generator runs in (the region throws; Galacticraft's moon dungeons read
     * past the chunks being decorated).
     */
    private boolean unloaded(int x, int z) {
        if (lvl() instanceof net.minecraft.server.level.WorldGenRegion region) return !region.hasChunk(x >> 4, z >> 4);
        return lvl() instanceof net.minecraft.server.level.ServerLevel sl && !sl.getChunkSource().hasChunk(x >> 4, z >> 4);
    }

    /**
     * A block of a server level read from another thread (a world generator holding an entity of the real world, such as
     * Immersive Engineering's village item frames): only a chunk that is fully loaded right now is read. The level itself
     * would wait for the chunk on the server thread, which may be waiting for this very generation (the server hung).
     * Null when the read happens on the server thread or in a generation region.
     */
    private BlockState mcc$offThreadState(int x, int y, int z) {
        if (!(lvl() instanceof net.minecraft.server.level.ServerLevel sl) || sl.getServer().isSameThread()) return null;
        var holder = sl.getChunkSource().chunkMap.getVisibleChunkIfPresent(net.minecraft.world.level.ChunkPos.asLong(x >> 4, z >> 4));
        var chunk = holder == null ? null : holder.getChunkIfPresent(net.minecraft.world.level.chunk.status.ChunkStatus.FULL);
        return chunk == null ? Blocks.AIR.defaultBlockState() : chunk.getBlockState(pos(x, y, z));
    }

    @Override
    public Block getBlock(int x, int y, int z) {
        if (unloaded(x, z)) return legacyconnector.v1_7_10.net.minecraft.init.Blocks.air;
        BlockState offThread = mcc$offThreadState(x, y, z);
        BlockState state = offThread != null ? offThread : lvl().getBlockState(pos(x, y, z));
        // Ore generators replace stone; the deep layer is deepslate.
        if (state.is(Blocks.DEEPSLATE) && inDeepPass()) return legacyconnector.v1_7_10.net.minecraft.init.Blocks.stone;
        if (mcc$bedrockFloor(y, state)) return legacyconnector.v1_7_10.net.minecraft.init.Blocks.bedrock;
        return BlockBridge.legacyBlock(state);
    }

    /**
     * 1.7.10's overworld always had bedrock at y=0, and mods rely on it: Traincraft's track pieces check their origin, which
     * is (0,0,0) until placement finishes, and break when it is air. Where a modern world extends below 0 and has air at
     * y=0 (superflat worlds, caves), the legacy view answers bedrock. Nether and End views (bottom 0) are unchanged.
     */
    private boolean mcc$bedrockFloor(int y, BlockState state) {
        return y == 0 && state.isAir() && lvl().getMinBuildHeight() < 0;
    }

    @Override
    public int getBlockMetadata(int x, int y, int z) {
        if (unloaded(x, z)) return 0;
        BlockState offThread = mcc$offThreadState(x, y, z);
        return BlockBridge.legacyMeta(offThread != null ? offThread : lvl().getBlockState(pos(x, y, z)));
    }

    /**
     * Legacy generators spill structures and trees past the chunk being populated; a generation region only accepts writes
     * within one chunk of its centre (and logs an error with a stack trace for each refused one, which is slow and noisy).
     */
    private boolean outsideRegion(BlockPos p) {
        if (p.getY() >= 0 && inDeepPass()) return true;
        if (lvl() instanceof net.minecraft.server.level.WorldGenRegion r) {
            var c = r.getCenter();
            return Math.abs((p.getX() >> 4) - c.x) > 1 || Math.abs((p.getZ() >> 4) - c.z) > 1;
        }
        return false;
    }

    /** An ore placed into deepslate (or over a deepslate ore) becomes the ore's deepslate variant. */
    private BlockState deepslateOre(BlockState state, BlockPos p) {
        if (!(state.getBlock() instanceof org.kylepitcock.legacyconnector.v1_7_10.adapter.LegacyBlockAdapter) && inDeepPass()) {
            // vanilla ores placed by legacy generators (CoFHWorld, Galacticraft...) into deepslate use the deepslate variant
            var key = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock());
            if ("minecraft".equals(key.getNamespace()) && key.getPath().endsWith("_ore") && !key.getPath().startsWith("deepslate_")
                    && lvl().getBlockState(p).is(Blocks.DEEPSLATE)) {
                var twin = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getOptional(
                        net.minecraft.resources.ResourceLocation.withDefaultNamespace("deepslate_" + key.getPath()));
                if (twin.isPresent()) return twin.get().defaultBlockState();
            }
            return state;
        }
        if (!(state.getBlock() instanceof org.kylepitcock.legacyconnector.v1_7_10.adapter.LegacyBlockAdapter a) || a.isDeepVariant()) return state;
        var twin = a.deepVariant();
        if (twin == null) return state;
        BlockState current = lvl().getBlockState(p);
        boolean deep = current.is(Blocks.DEEPSLATE) || current.getBlock() == twin;
        return deep ? twin.stateForMeta(state.getValue(org.kylepitcock.legacyconnector.v1_7_10.adapter.LegacyBlockAdapter.META)) : state;
    }

    public boolean setBlock(int x, int y, int z, Block block, int meta, int flags) {
        BlockState state = BlockBridge.toModern(block, meta);
        BlockPos p = pos(x, y, z);
        if (outsideRegion(p)) return false;
        state = deepslateOre(state, p);
        TileEntityBridge.beforeBlockChange(this, p);
        TileEntityBridge.beforeSetBlock(this, p, state);
        if (connectsToNeighbours(state)) {
            if (lvl() instanceof net.minecraft.server.level.WorldGenRegion region) {
                // Neighbours may still be missing; the game fixes the shape when the chunk is finished, as for structures.
                boolean placed = region.setBlock(p, state, flags);
                if (placed) region.getChunk(p).markPosForPostprocessing(p);
                ensureGeneratedBlockEntity(region, p, state);
                return placed;
            }
            state = net.minecraft.world.level.block.Block.updateFromNeighbourShapes(state, lvl(), p);
        }
        boolean placed = lvl().setBlock(p, state, flags);
        if (placed && lvl() instanceof net.minecraft.server.level.WorldGenRegion region) ensureGeneratedBlockEntity(region, p, state);
        return placed;
    }

    /**
     * While a chunk is generated the game only records a stub for a placed block entity (it is created when the chunk is finished),
     * but legacy generators fetch the tile entity right after placing the block to fill it (chest loot, spawner mob).
     */
    private static void ensureGeneratedBlockEntity(net.minecraft.server.level.WorldGenRegion region, BlockPos p, BlockState state) {
        if (!state.hasBlockEntity() || !(state.getBlock() instanceof net.minecraft.world.level.block.EntityBlock entityBlock)) return;
        var chunk = region.getChunk(p);
        if (chunk.getBlockEntity(p) != null) return;
        var be = entityBlock.newBlockEntity(p, state);
        if (be != null) chunk.setBlockEntity(be);
    }

    /**
     * Glass panes, iron bars, fences, walls and stairs keep their connections in the modern block state; 1.7.10 worked
     * them out while drawing, so legacy code (structure generators) only places the block and it would stand alone.
     * Doors are left out: their shape update removes a half placed before its partner.
     */
    private static boolean connectsToNeighbours(BlockState state) {
        var b = state.getBlock();
        return b instanceof net.minecraft.world.level.block.CrossCollisionBlock || b instanceof net.minecraft.world.level.block.WallBlock
                || b instanceof net.minecraft.world.level.block.StairBlock;
    }

    public boolean setBlock(int x, int y, int z, Block block) {
        return setBlock(x, y, z, block, 0, 3);
    }

    public boolean setBlockMetadataWithNotify(int x, int y, int z, int meta, int flags) {
        BlockPos p = pos(x, y, z);
        if (outsideRegion(p)) return false;
        BlockState current = lvl().getBlockState(p);
        BlockState next = BlockBridge.withMeta(current, meta);
        if (next == current) return false;
        return lvl().setBlock(p, next, flags);
    }

    public boolean setBlockToAir(int x, int y, int z) {
        if (outsideRegion(pos(x, y, z))) return false;
        TileEntityBridge.beforeBlockChange(this, pos(x, y, z));
        return lvl().setBlock(pos(x, y, z), Blocks.AIR.defaultBlockState(), 3);
    }

    /** {@code func_147480_a}: break a block, optionally dropping its items. */
    public boolean breakBlock(int x, int y, int z, boolean drop) {
        if (outsideRegion(pos(x, y, z))) return false;
        TileEntityBridge.beforeBlockChange(this, pos(x, y, z));
        return lvl().destroyBlock(pos(x, y, z), drop);
    }

    @Override
    public boolean isAirBlock(int x, int y, int z) {
        BlockState state = lvl().getBlockState(pos(x, y, z));
        return state.isAir() && !mcc$bedrockFloor(y, state);
    }

    public boolean blockExists(int x, int y, int z) {
        return lvl().hasChunkAt(pos(x, y, z));
    }

    public boolean checkChunksExist(int x1, int y1, int z1, int x2, int y2, int z2) {
        return lvl().hasChunksAt(x1, z1, x2, z2);
    }

    public boolean doChunksNearChunkExist(int x, int y, int z, int range) {
        return checkChunksExist(x - range, y - range, z - range, x + range, y + range, z + range);
    }

    public boolean isBlockNormalCubeDefault(int x, int y, int z, boolean def) {
        BlockPos p = pos(x, y, z);
        if (!lvl().hasChunkAt(p)) return def;
        return lvl().getBlockState(p).isRedstoneConductor(lvl(), p);
    }

    public boolean isBlockFullCube(int x, int y, int z) {
        BlockPos p = pos(x, y, z);
        return lvl().getBlockState(p).isCollisionShapeFullBlock(lvl(), p);
    }

    @Override
    public boolean isSideSolid(int x, int y, int z, ForgeDirection side, boolean defaultValue) {
        BlockPos p = pos(x, y, z);
        if (!lvl().hasChunkAt(p)) return defaultValue;
        Direction d = side.mcc$modern();
        return d != null && lvl().getBlockState(p).isFaceSturdy(lvl(), p, d);
    }

    public boolean isSideSolid(int x, int y, int z, ForgeDirection side) {
        return isSideSolid(x, y, z, side, false);
    }

    public static boolean doesBlockHaveSolidTopSurface(IBlockAccess world, int x, int y, int z) {
        return world.isSideSolid(x, y, z, ForgeDirection.UP, false);
    }

    public Block getTopBlock(int x, int z) {
        int y = getHeightValue(x, z);
        return getBlock(x, y - 1, z);
    }

    /**
     * Height of the first block that stops motion or holds fluid, scanning down the column. While a chunk is being
     * generated only the generation heightmaps exist (MOTION_BLOCKING reads as unprimed there), so decoration and
     * structure code that asks for the surface must be answered from the blocks themselves.
     */
    private int surface(int x, int z, Heightmap.Types type) {
        return legacyY(modernSurface(x, z, type));
    }

    private int modernSurface(int x, int z, Heightmap.Types type) {
        LevelAccessor level = lvl();
        if (level instanceof net.minecraft.server.level.WorldGenRegion region) {
            if (!region.hasChunk(x >> 4, z >> 4)) return region.getSeaLevel();
            BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
            boolean noLeaves = type == Heightmap.Types.MOTION_BLOCKING_NO_LEAVES;
            for (int y = region.getMaxBuildHeight() - 1; y >= region.getMinBuildHeight(); y--) {
                var state = region.getBlockState(p.set(x, y, z));
                if (state.isAir()) continue;
                if (noLeaves && state.is(net.minecraft.tags.BlockTags.LEAVES)) continue;
                if (state.blocksMotion() || !state.getFluidState().isEmpty()) return y + 1;
            }
            return region.getMinBuildHeight();
        }
        return level.getHeight(type, x, z);
    }

    public int getHeightValue(int x, int z) {
        return surface(x, z, Heightmap.Types.MOTION_BLOCKING);
    }

    public int getTopSolidOrLiquidBlock(int x, int z) {
        return surface(x, z, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES);
    }

    public int getPrecipitationHeight(int x, int z) {
        return surface(x, z, Heightmap.Types.MOTION_BLOCKING);
    }

    public boolean canBlockSeeTheSky(int x, int y, int z) {
        return lvl().canSeeSky(pos(x, y, z));
    }

    /** Brightness 0..1 (light level scaled by the dimension's ambient light curve). */
    public float getLightBrightness(int x, int y, int z) {
        return lvl().getLightLevelDependentMagicValue(pos(x, y, z));
    }

    public int getBlockLightValue(int x, int y, int z) {
        return lvl().getMaxLocalRawBrightness(pos(x, y, z));
    }

    public int getSavedLightValue(EnumSkyBlock type, int x, int y, int z) {
        return lvl().getBrightness(type == EnumSkyBlock.Sky ? net.minecraft.world.level.LightLayer.SKY : net.minecraft.world.level.LightLayer.BLOCK, pos(x, y, z));
    }

    public boolean updateAllLightTypes(int x, int y, int z) {
        return false;
    }

    public legacyconnector.v1_7_10.net.minecraft.world.storage.ISaveHandler getSaveHandler() {
        Level level = mcc$fullLevel();
        java.io.File dir = level instanceof ServerLevel s ? s.getServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).toFile() : new java.io.File(".");
        return new legacyconnector.v1_7_10.net.minecraft.world.storage.SaveHandler(dir, dir.getName());
    }

    /** Whether the player may break blocks here; the provider decides (by default {@link #canMineBlockBody}: spawn protection). */
    public boolean canMineBlock(EntityPlayer player, int x, int y, int z) {
        return mcc$get$provider(this).canMineBlock(player, x, y, z);
    }

    public float getWeightedThunderStrength(float partialTicks) {
        Level level = mcc$liveLevel();
        return level == null ? 0.0F : level.getThunderLevel(partialTicks);
    }

    public int getFullBlockLightValue(int x, int y, int z) {
        return getBlockLightValue(x, y, z);
    }

    @Override
    public int getLightBrightnessForSkyBlocks(int x, int y, int z, int min) {
        BlockPos p = pos(x, y, z);
        int sky = lvl().getBrightness(net.minecraft.world.level.LightLayer.SKY, p);
        int block = Math.max(min, lvl().getBrightness(net.minecraft.world.level.LightLayer.BLOCK, p));
        return sky << 20 | block << 4;
    }

    /**
     * The world's height for legacy code: the modern level's top (320 in the overworld, 256 in the nether, end and mod dimensions)
     * where players act, so checks like RotaryCraft's {@code y < world.provider.getHeight() - 1} allow the whole build range.
     * Generation views keep 1.7.10's 256: generators pick heights with {@code nextInt(world.getHeight())}.
     */
    @Override
    public int getHeight() {
        return mcc$buildTop();
    }

    /**
     * The lowest y blocks can be at: the modern level's bottom (-64 in the overworld), 0 in generation views (legacy generators
     * see 0..255; the deep pass shifts the world so that 0 is the modern bottom) and in levels that start at 0. The loader turns
     * 1.7.10's hard-coded {@code y < 0} checks on a method's y parameter into a call here (BuildHeightRewriter, BuildHeight).
     */
    public int mcc$buildBottom() {
        LevelAccessor level = lvl();
        return level == null || mcc$legacyRange(level) ? 0 : level.getMinBuildHeight();
    }

    /** One above the highest y blocks can be at (see {@link #mcc$buildBottom}); 256 in generation views. */
    public int mcc$buildTop() {
        LevelAccessor level = lvl();
        return level == null || mcc$legacyRange(level) ? 256 : level.getMaxBuildHeight();
    }

    private static boolean mcc$legacyRange(LevelAccessor level) {
        return inDeepPass() || !(level instanceof Level);
    }

    public int getActualHeight() {
        return mcc$get$provider(this).getActualHeight();
    }

    @Override
    public boolean extendedLevelsInChunkCache() {
        return false;
    }

    // ------------------------------------------------------------------ updates and redstone

    public void markBlockForUpdate(int x, int y, int z) {
        Level level = mcc$liveLevel();
        if (level == null) return;
        // Generation workers (lighting passes of mod chunk providers) must not read the live level: that waits for the
        // main thread, which may be waiting for the chunk being generated. Nothing is sent to clients for them yet anyway.
        if (level instanceof net.minecraft.server.level.ServerLevel sl && !sl.getServer().isSameThread()) return;
        // Nor a chunk that is not loaded: no client sees it, and reading it waits for a load that never finishes when the
        // chunk is the one unloading (Thaumcraft's vis relays mark their neighbours from invalidate(): the shutdown save hung)
        if (level instanceof net.minecraft.server.level.ServerLevel sl && !sl.getChunkSource().hasChunk(x >> 4, z >> 4)) return;
        BlockPos p = pos(x, y, z);
        BlockState s = level.getBlockState(p);
        level.sendBlockUpdated(p, s, s, 3);
    }

    public void markBlockForRenderUpdate(int x, int y, int z) {
        markBlockForUpdate(x, y, z);
    }

    public void markBlockRangeForRenderUpdate(int x1, int y1, int z1, int x2, int y2, int z2) {
    }

    public void markTileEntityChunkModified(int x, int y, int z, TileEntity te) {
        Level level = mcc$liveLevel();
        if (level != null) level.blockEntityChanged(pos(x, y, z));
    }

    public void notifyBlocksOfNeighborChange(int x, int y, int z, Block block) {
        lvl().blockUpdated(pos(x, y, z), BlockBridge.modernBlock(block));
    }

    public void notifyBlocksOfNeighborChange(int x, int y, int z, Block block, int skipSide) {
        Level level = mcc$liveLevel();
        if (level == null) return;
        Direction skip = skipSide >= 0 && skipSide < 6 ? Direction.from3DDataValue(skipSide) : null;
        level.updateNeighborsAtExceptFromFacing(pos(x, y, z), BlockBridge.modernBlock(block), skip);
    }

    public void notifyBlockOfNeighborChange(int x, int y, int z, Block block) {
        Level level = mcc$liveLevel();
        if (level != null) level.neighborChanged(pos(x, y, z), BlockBridge.modernBlock(block), pos(x, y, z));
    }

    public void notifyBlockChange(int x, int y, int z, Block block) {
        notifyBlocksOfNeighborChange(x, y, z, block);
    }

    public void func_147453_f(int x, int y, int z, Block block) {
        Level level = mcc$liveLevel();
        if (level != null) level.updateNeighbourForOutputSignal(pos(x, y, z), BlockBridge.modernBlock(block));
    }

    public void scheduleBlockUpdate(int x, int y, int z, Block block, int delay) {
        var modern = BlockBridge.modernBlock(block);
        lvl().scheduleTick(pos(x, y, z), modern, delay);
        // A vanilla plant's growth is its random tick now, which a scheduled tick does not run: legacy code scheduling one to
        // grow it (Thaumcraft's Lamp of Growth) gets the 1.7.10 updateTick when the delay is up.
        if (block != null && block.mcc$vanilla != null && modern.defaultBlockState().isRandomlyTicking()
                && mcc$liveLevel() instanceof net.minecraft.server.level.ServerLevel level) {
            var server = level.getServer();
            server.tell(new net.minecraft.server.TickTask(server.getTickCount() + Math.max(1, delay),
                    () -> block.updateTick(this, x, y, z, rand)));
        }
    }

    public void scheduleBlockUpdateWithPriority(int x, int y, int z, Block block, int delay, int priority) {
        scheduleBlockUpdate(x, y, z, block, delay);
    }

    public void func_147454_a(int x, int y, int z, Block block, int delay, int priority) {
        scheduleBlockUpdate(x, y, z, block, delay);
    }

    public boolean isBlockTickScheduledThisTick(int x, int y, int z, Block block) {
        return lvl().getBlockTicks().willTickThisTick(pos(x, y, z), BlockBridge.modernBlock(block));
    }

    public void addBlockEvent(int x, int y, int z, Block block, int id, int param) {
        Level level = mcc$liveLevel();
        if (level != null) level.blockEvent(pos(x, y, z), BlockBridge.modernBlock(block), id, param);
    }

    public boolean isBlockIndirectlyGettingPowered(int x, int y, int z) {
        return lvl() instanceof SignalGetter s && s.hasNeighborSignal(pos(x, y, z));
    }

    public int getStrongestIndirectPower(int x, int y, int z) {
        return lvl() instanceof SignalGetter s ? s.getBestNeighborSignal(pos(x, y, z)) : 0;
    }

    public boolean getIndirectPowerOutput(int x, int y, int z, int side) {
        return getIndirectPowerLevelTo(x, y, z, side) > 0;
    }

    public int getIndirectPowerLevelTo(int x, int y, int z, int side) {
        return lvl() instanceof SignalGetter s ? s.getSignal(pos(x, y, z), Direction.from3DDataValue(side)) : 0;
    }

    public int getBlockPowerInput(int x, int y, int z) {
        return lvl() instanceof SignalGetter s ? s.getDirectSignalTo(pos(x, y, z)) : 0;
    }

    @Override
    public int isBlockProvidingPowerTo(int x, int y, int z, int side) {
        BlockPos p = pos(x, y, z);
        return lvl().getBlockState(p).getDirectSignal(lvl(), p, Direction.from3DDataValue(side));
    }

    // ------------------------------------------------------------------ tile entities

    @Override
    public TileEntity getTileEntity(int x, int y, int z) {
        if (outsideRegion(pos(x, y, z))) return null;
        return TileEntityBridge.legacyAt(this, pos(x, y, z));
    }

    public void setTileEntity(int x, int y, int z, TileEntity te) {
        if (outsideRegion(pos(x, y, z))) return;
        TileEntityBridge.setAt(this, pos(x, y, z), te);
    }

    public void removeTileEntity(int x, int y, int z) {
        if (outsideRegion(pos(x, y, z))) return;
        lvl().getChunk(pos(x, y, z)).removeBlockEntity(pos(x, y, z));
    }

    // ------------------------------------------------------------------ entities

    public boolean spawnEntityInWorld(Entity entity) {
        if (inDeepPass()) return false; // entity positions are not shifted
        if (entity instanceof legacyconnector.v1_7_10.net.minecraft.entity.effect.EntityLightningBolt) return addWeatherEffect(entity);
        boolean spawned = EntityBridge.spawn(this, entity);
        if (spawned && mcc$entityList != null) mcc$entityList.add(entity);
        return spawned;
    }

    public void removeEntity(Entity entity) {
        entity.setDead();
    }

    public EntityPlayer getClosestPlayer(double x, double y, double z, double distance) {
        var p = lvl().getNearestPlayer(x, y, z, distance, false);
        return p == null ? null : (EntityPlayer) EntityBridge.wrap(p);
    }

    public EntityPlayer getClosestPlayerToEntity(Entity entity, double distance) {
        return getClosestPlayer(entity.mcc$x(), entity.mcc$y(), entity.mcc$z(), distance);
    }

    public EntityPlayer getClosestVulnerablePlayer(double x, double y, double z, double distance) {
        var p = lvl().getNearestPlayer(x, y, z, distance, true);
        return p == null ? null : (EntityPlayer) EntityBridge.wrap(p);
    }

    public EntityPlayer getClosestVulnerablePlayerToEntity(Entity entity, double distance) {
        return getClosestVulnerablePlayer(entity.mcc$x(), entity.mcc$y(), entity.mcc$z(), distance);
    }

    /** The legacy biome at a column (the modern biome at its surface, as its legacy counterpart). */
    /** Reseeds {@link #rand} for a position (structures and generators use it for repeatable placement). */
    public java.util.Random setRandomSeed(int x, int z, int salt) {
        long seed = x * 341873128712L + z * 132897987541L + getSeed() + salt;
        rand.setSeed(seed);
        return rand;
    }

    private legacyconnector.v1_7_10.net.minecraft.world.biome.WorldChunkManager mcc$chunkManager;

    public legacyconnector.v1_7_10.net.minecraft.world.biome.WorldChunkManager getWorldChunkManager() {
        if (mcc$chunkManager == null) {
            // A mod dimension's provider has its own biome layers (1.7.10: provider.worldChunkMgr).
            var provided = mcc$get$provider(this).worldChunkMgr;
            mcc$chunkManager = provided != null ? provided
                    : new legacyconnector.v1_7_10.net.minecraft.world.biome.WorldChunkManager.WorldView(this);
        }
        return mcc$chunkManager;
    }

    public legacyconnector.v1_7_10.net.minecraft.world.biome.BiomeGenBase getBiomeGenForCoords(int x, int z) {
        // A mod dimension's biomes come from its provider's layers (not from the chunks, which may not exist yet).
        var provided = mcc$get$provider(this).worldChunkMgr;
        if (provided != null && !(provided instanceof legacyconnector.v1_7_10.net.minecraft.world.biome.WorldChunkManager.WorldView)) {
            legacyconnector.v1_7_10.net.minecraft.world.biome.BiomeGenBase biome;
            synchronized (provided) {
                biome = provided.getBiomeGenAt(x, z);
            }
            if (biome != null) return biome;
        }
        // Outside a generation region's chunks there is no heightmap to read: sample at sea level.
        // A generation worker must not ask the real level for a chunk: that waits on the main thread, which may be
        // waiting for this very chunk (a self-test or player load that generates synchronously).
        boolean offThread = lvl() instanceof net.minecraft.server.level.ServerLevel sl && !sl.getServer().isSameThread();
        if (offThread) {
            var holder = generatorBiome(x, lvl().getSeaLevel(), z);
            var key = holder == null ? null : holder.unwrapKey().map(k -> k.location()).orElse(null);
            var legacy = org.kylepitcock.legacyconnector.v1_7_10.bridge.LegacyEntitySpawns.legacyBiome(key);
            return legacy != null ? legacy : legacyconnector.v1_7_10.net.minecraft.world.biome.BiomeGenBase.plains;
        }
        // a server level's chunk counts only when fully loaded now: hasChunk is also true for a chunk still being promoted,
        // and reading its height waits for it (Thaumcraft's entity-join handler asks for the biome of an entity of the chunk
        // being loaded, which hung the server)
        boolean present = lvl() instanceof net.minecraft.server.level.WorldGenRegion region
                ? region.getCenter().x == x >> 4 && region.getCenter().z == z >> 4
                : lvl() instanceof net.minecraft.server.level.ServerLevel sl
                        ? sl.getChunkSource().getChunkNow(x >> 4, z >> 4) != null
                        : lvl().hasChunk(x >> 4, z >> 4);
        // full chunks drop the _WG heightmaps: reading one logs "Unprimed heightmap" (CustomOreGen's debug wireframes, per sample)
        var heightmap = lvl() instanceof net.minecraft.server.level.WorldGenRegion
                ? net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE_WG : net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE;
        int y = present ? lvl().getHeight(heightmap, x, z) : lvl().getSeaLevel();
        net.minecraft.core.Holder<net.minecraft.world.level.biome.Biome> holder;
        try {
            // outside a generation region, ask the generator directly: the region throws, after building a whole crash report
            // with a system report (CustomOreGen samples biomes far around each chunk: generation took an hour)
            // (inside it, the chunk's own biome: getBiome's zoom samples neighbours, which may lie outside the region)
            if (!(lvl() instanceof net.minecraft.server.level.WorldGenRegion)) holder = lvl().getBiome(pos(x, y, z));
            // only the centre chunk is safe to read: hasChunk covers the whole cache, getChunk only the step's radius
            else holder = present ? lvl().getNoiseBiome(x >> 2, y >> 2, z >> 2) : generatorBiome(x, y, z);
        } catch (RuntimeException e) {
            // A generation region only holds its own chunks: ask the generator's biome source (what vanilla
            // 1.7.10's chunk manager answered for any coordinate).
            holder = generatorBiome(x, y, z);
        }
        var key = holder == null ? null : holder.unwrapKey().map(k -> k.location()).orElse(null);
        var legacy = org.kylepitcock.legacyconnector.v1_7_10.bridge.LegacyEntitySpawns.legacyBiome(key);
        return legacy != null ? legacy : legacyconnector.v1_7_10.net.minecraft.world.biome.BiomeGenBase.plains;
    }

    private net.minecraft.core.Holder<net.minecraft.world.level.biome.Biome> generatorBiome(int x, int y, int z) {
        try {
            if (mcc$fullLevel() instanceof net.minecraft.server.level.ServerLevel sl) {
                var cache = sl.getChunkSource();
                return cache.getGenerator().getBiomeSource().getNoiseBiome(x >> 2, y >> 2, z >> 2, cache.randomState().sampler());
            }
        } catch (RuntimeException e) {
            // fall through
        }
        return null;
    }

    public legacyconnector.v1_7_10.net.minecraft.world.biome.BiomeGenBase getBiomeGenForCoordsBody(int x, int z) {
        return getBiomeGenForCoords(x, z);
    }

    public legacyconnector.v1_7_10.net.minecraft.world.chunk.Chunk getChunkFromBlockCoords(int x, int z) {
        return getChunkFromChunkCoords(x >> 4, z >> 4);
    }

    /**
     * A view of the chunk. On the server thread of a running world a chunk that is not loaded is loaded or generated, as
     * 1.7.10's chunk provider did (Extra Utilities' teleporter scans the chunks around its destination for a portal and
     * failed on a null one); elsewhere (a generation region, another thread) null when it is not available.
     */
    public legacyconnector.v1_7_10.net.minecraft.world.chunk.Chunk getChunkFromChunkCoords(int cx, int cz) {
        var access = lvl().getChunk(cx, cz, net.minecraft.world.level.chunk.status.ChunkStatus.EMPTY, false);
        if (access == null && lvl() == mcc$liveLevel() && mcc$liveLevel() instanceof net.minecraft.server.level.ServerLevel level && level.getServer().isSameThread()) {
            access = level.getChunk(cx, cz);
        }
        return access == null ? null : new legacyconnector.v1_7_10.net.minecraft.world.chunk.Chunk(this, access);
    }

    public boolean chunkExists(int cx, int cz) {
        return lvl().hasChunk(cx, cz);
    }

    /** The world's chunks as a 1.7.10 chunk provider (only chunks that are loaded or generating here are provided). */
    public legacyconnector.v1_7_10.net.minecraft.world.chunk.IChunkProvider getChunkProvider() {
        var provider = mcc$chunkProvider;
        if (provider == null) mcc$chunkProvider = provider = new legacyconnector.v1_7_10.net.minecraft.world.gen.ChunkProviderServer(this);
        return provider;
    }

    /** One provider per world, as in 1.7.10 (mods keep it and read its fields). */
    private legacyconnector.v1_7_10.net.minecraft.world.gen.ChunkProviderServer mcc$chunkProvider;

    /** A provider a mod installs ({@code theChunkProviderServer = ...}). */
    public void mcc$setChunkProvider(legacyconnector.v1_7_10.net.minecraft.world.gen.ChunkProviderServer provider) {
        mcc$chunkProvider = provider;
    }

    public EntityPlayer func_152378_a(java.util.UUID id) {
        var p = id == null ? null : lvl().getPlayerByUUID(id);
        return p == null ? null : (EntityPlayer) EntityBridge.wrap(p);
    }

    public EntityPlayer getPlayerEntityByName(String name) {
        for (var p : lvl().players()) {
            if (p.getGameProfile().getName().equals(name)) return (EntityPlayer) EntityBridge.wrap(p);
        }
        return null;
    }

    public EntityPlayer getPlayerEntityByUUID(java.util.UUID uuid) {
        var p = lvl().getPlayerByUUID(uuid);
        return p == null ? null : (EntityPlayer) EntityBridge.wrap(p);
    }

    public Entity getEntityByID(int id) {
        Level level = mcc$liveLevel();
        if (level == null) return null;
        var e = level.getEntity(id);
        return e == null ? null : EntityBridge.wrap(e);
    }

    public legacyconnector.v1_7_10.net.minecraft.util.MovingObjectPosition rayTraceBlocks(legacyconnector.v1_7_10.net.minecraft.util.Vec3 start, legacyconnector.v1_7_10.net.minecraft.util.Vec3 end) {
        return rayTraceBlocks(start, end, false, false, false);
    }

    public legacyconnector.v1_7_10.net.minecraft.util.MovingObjectPosition rayTraceBlocks(legacyconnector.v1_7_10.net.minecraft.util.Vec3 start, legacyconnector.v1_7_10.net.minecraft.util.Vec3 end, boolean liquids) {
        return rayTraceBlocks(start, end, liquids, false, false);
    }

    /**
     * Null on a miss, as in 1.7.10. The modern clip stands in for the block-by-block walk over each block's collisionRayTrace:
     * by default every block is hit through its selection box (OUTLINE: nodes, torches, flowers, which items such as
     * Thaumcraft's wand target); {@code ignoreNoBoundingBox} skips blocks without a collision box (COLLIDER, projectiles).
     */
    public legacyconnector.v1_7_10.net.minecraft.util.MovingObjectPosition rayTraceBlocks(legacyconnector.v1_7_10.net.minecraft.util.Vec3 start,
            legacyconnector.v1_7_10.net.minecraft.util.Vec3 end, boolean liquids, boolean ignoreNoBoundingBox, boolean returnLastUncollidable) {
        var hit = lvl().clip(new net.minecraft.world.level.ClipContext(start.mcc$modern(), end.mcc$modern(),
                ignoreNoBoundingBox ? net.minecraft.world.level.ClipContext.Block.COLLIDER : net.minecraft.world.level.ClipContext.Block.OUTLINE,
                liquids ? net.minecraft.world.level.ClipContext.Fluid.ANY : net.minecraft.world.level.ClipContext.Fluid.NONE,
                net.minecraft.world.phys.shapes.CollisionContext.empty()));
        if (liquids) {
            var source = mcc$legacyLiquidSource(start, end, hit.getType() == net.minecraft.world.phys.HitResult.Type.MISS ? null : hit.getLocation());
            if (source != null) return source;
        }
        if (hit.getType() == net.minecraft.world.phys.HitResult.Type.MISS) return null;
        var pos = hit.getBlockPos();
        var at = hit.getLocation();
        return new legacyconnector.v1_7_10.net.minecraft.util.MovingObjectPosition(pos.getX(), pos.getY(), pos.getZ(), hit.getDirection().get3DDataValue(),
                legacyconnector.v1_7_10.net.minecraft.util.Vec3.createVectorHelper(at.x, at.y, at.z));
    }

    /**
     * The first source block (meta 0) of a legacy liquid on the ray before {@code before}: mod fluids have no modern fluid state,
     * so the fluid clip does not see them, but 1.7.10's trace with stopOnLiquid stopped there (buckets, fluid pickers).
     */
    private legacyconnector.v1_7_10.net.minecraft.util.MovingObjectPosition mcc$legacyLiquidSource(legacyconnector.v1_7_10.net.minecraft.util.Vec3 start,
            legacyconnector.v1_7_10.net.minecraft.util.Vec3 end, net.minecraft.world.phys.Vec3 before) {
        var from = start.mcc$modern();
        var to = before != null ? before : end.mcc$modern();
        double length = from.distanceTo(to);
        if (length <= 0 || length > 512) return null;
        var dir = to.subtract(from).normalize();
        var cursor = new net.minecraft.core.BlockPos.MutableBlockPos();
        long last = Long.MIN_VALUE;
        for (double d = 0; d <= length; d += 0.05) {
            var at = from.add(dir.scale(d));
            cursor.set(at.x, at.y, at.z);
            if (cursor.asLong() == last) continue;
            last = cursor.asLong();
            var state = lvl().getBlockState(cursor);
            if (state.getBlock() instanceof org.kylepitcock.legacyconnector.v1_7_10.adapter.LegacyBlockAdapter && state.liquid()
                    && org.kylepitcock.legacyconnector.v1_7_10.bridge.BlockBridge.legacyMeta(state) == 0) {
                // the face entered: the axis whose cell boundary was crossed last
                var prev = from.add(dir.scale(Math.max(0, d - 0.05)));
                int side = Math.floor(prev.y) != cursor.getY() ? (prev.y < cursor.getY() ? 0 : 1)
                        : Math.floor(prev.z) != cursor.getZ() ? (prev.z < cursor.getZ() ? 2 : 3) : prev.x < cursor.getX() ? 4 : 5;
                return new legacyconnector.v1_7_10.net.minecraft.util.MovingObjectPosition(cursor.getX(), cursor.getY(), cursor.getZ(), side,
                        legacyconnector.v1_7_10.net.minecraft.util.Vec3.createVectorHelper(at.x, at.y, at.z));
            }
        }
        return null;
    }

    /** Boxes of the blocks colliding with {@code box} (entity colliders such as boats are not included). */
    public List getCollidingBoundingBoxes(Entity entity, AxisAlignedBB box) {
        List<AxisAlignedBB> out = new ArrayList<>();
        var modernEntity = entity == null ? null : entity.mcc$entity();
        for (var shape : lvl().getBlockCollisions(modernEntity, box.mcc$modern())) {
            for (AABB part : shape.toAabbs()) out.add(AxisAlignedBB.mcc$of(part));
        }
        return out;
    }

    @Override
    public int mcc$blockTint(int x, int y, int z, int kind) {
        return BlockAccessView.tint(lvl(), x, y, z, kind);
    }

    public List getEntitiesWithinAABB(Class type, AxisAlignedBB box) {
        return selectEntitiesWithinAABB(type, box, null);
    }

    public List selectEntitiesWithinAABB(Class type, AxisAlignedBB box, legacyconnector.v1_7_10.net.minecraft.command.IEntitySelector selector) {
        List<Entity> out = new ArrayList<>();
        AABB modern = box.mcc$modern();
        for (var e : lvl().getEntities((net.minecraft.world.entity.Entity) null, modern, World::visibleToLegacy)) {
            Entity legacy = EntityBridge.wrap(e);
            if (type.isInstance(legacy) && (selector == null || selector.isEntityApplicable(legacy))) out.add(legacy);
        }
        return out;
    }

    public List getEntitiesWithinAABBExcludingEntity(Entity except, AxisAlignedBB box) {
        List<Entity> out = new ArrayList<>();
        var exceptModern = except == null ? null : except.mcc$entity();
        for (var e : lvl().getEntities(exceptModern, box.mcc$modern(), World::visibleToLegacy)) out.add(EntityBridge.wrap(e));
        return out;
    }

    /**
     * Item entities whose stack is empty (merged into another stack, removed on the next tick) are not reported: in 1.7.10 an
     * item entity always held a stack, and mods read {@code getEntityItem().getItem()} without a check.
     */
    private static boolean visibleToLegacy(net.minecraft.world.entity.Entity e) {
        return !(e instanceof net.minecraft.world.entity.item.ItemEntity item) || !item.getItem().isEmpty();
    }

    public List getEntitiesWithinAABBExcludingEntity(Entity except, AxisAlignedBB box, legacyconnector.v1_7_10.net.minecraft.command.IEntitySelector selector) {
        List<Entity> out = new ArrayList<>();
        for (Object o : getEntitiesWithinAABBExcludingEntity(except, box)) {
            if (selector == null || selector.isEntityApplicable((Entity) o)) out.add((Entity) o);
        }
        return out;
    }

    public Entity findNearestEntityWithinAABB(Class type, AxisAlignedBB box, Entity entity) {
        Entity best = null;
        double bestDist = Double.MAX_VALUE;
        for (Object o : getEntitiesWithinAABB(type, box)) {
            Entity e = (Entity) o;
            if (e == entity) continue;
            double d = e.getDistanceSqToEntity(entity);
            if (d <= bestDist) {
                best = e;
                bestDist = d;
            }
        }
        return best;
    }

    public int countEntities(Class type) {
        int n = 0;
        for (Object o : mcc$get$loadedEntityList(this)) if (type.isInstance(o)) n++;
        return n;
    }

    public boolean checkNoEntityCollision(AxisAlignedBB box) {
        return checkNoEntityCollision(box, null);
    }

    public boolean checkNoEntityCollision(AxisAlignedBB box, Entity except) {
        var exceptModern = except == null ? null : except.mcc$entity();
        return lvl().isUnobstructed(exceptModern, net.minecraft.world.phys.shapes.Shapes.create(box.mcc$modern()));
    }

    public boolean canPlaceEntityOnSide(Block block, int x, int y, int z, boolean skipCollision, int side, Entity placer, ItemStack stack) {
        Block existing = getBlock(x, y, z);
        AxisAlignedBB box = skipCollision ? null : block.getCollisionBoundingBoxFromPool(this, x, y, z);
        if (box != null && !checkNoEntityCollision(box, placer)) return false;
        return existing.isReplaceable(this, x, y, z) && block.canPlaceBlockOnSide(this, x, y, z, side);
    }

    // ------------------------------------------------------------------ sound, particles, explosions

    public void playSoundEffect(double x, double y, double z, String name, float volume, float pitch) {
        Level level = mcc$liveLevel();
        if (level != null) SoundBridge.play(level, null, x, y, z, name, volume, pitch);
    }

    public void playSoundAtEntity(Entity entity, String name, float volume, float pitch) {
        Level level = mcc$liveLevel();
        if (level != null) SoundBridge.play(level, null, entity.mcc$x(), entity.mcc$y(), entity.mcc$z(), name, volume, pitch);
    }

    public void playSoundToNearExcept(EntityPlayer player, String name, float volume, float pitch) {
        Level level = mcc$liveLevel();
        if (level != null) SoundBridge.play(level, player.mcc$player(), player.mcc$x(), player.mcc$y(), player.mcc$z(), name, volume, pitch);
    }

    public void playSound(double x, double y, double z, String name, float volume, float pitch, boolean distanceDelay) {
        playSoundEffect(x, y, z, name, volume, pitch);
    }

    /** A world effect by its 1.7.10 id; the ids (and some data values) changed, so it is translated to the modern level event. */
    public void playAuxSFX(int type, int x, int y, int z, int data) {
        playAuxSFXAtEntity(null, type, x, y, z, data);
    }

    public void playAuxSFXAtEntity(EntityPlayer player, int type, int x, int y, int z, int data) {
        int[] modern = org.kylepitcock.legacyconnector.v1_7_10.bridge.LevelEvents1710.toModern(type, data);
        if (modern == null) return;
        lvl().levelEvent(player == null ? null : player.mcc$player(), modern[0], pos(x, y, z), modern[1]);
    }

    public void spawnParticle(String name, double x, double y, double z, double vx, double vy, double vz) {
        SoundBridge.particle(lvl(), name, x, y, z, vx, vy, vz);
    }

    public Explosion createExplosion(Entity entity, double x, double y, double z, float strength, boolean smoke) {
        return newExplosion(entity, x, y, z, strength, false, smoke);
    }

    public Explosion newExplosion(Entity entity, double x, double y, double z, float strength, boolean fire, boolean smoke) {
        Level level = mcc$liveLevel();
        if (level == null) return new Explosion(this, entity, x, y, z, strength);
        level.explode(entity == null ? null : entity.mcc$entity(), x, y, z, strength, fire,
                smoke ? Level.ExplosionInteraction.TNT : Level.ExplosionInteraction.NONE);
        return new Explosion(this, entity, x, y, z, strength);
    }

    // ------------------------------------------------------------------ time and weather

    public long getTotalWorldTime() {
        return lvl().getLevelData().getGameTime();
    }

    public long getWorldTime() {
        return lvl().getLevelData().getDayTime();
    }

    public void setWorldTime(long time) {
        if (mcc$fullLevel() instanceof ServerLevel s) s.setDayTime(time);
    }

    public boolean isDaytime() {
        Level level = mcc$fullLevel();
        return level != null && level.isDay();
    }

    /** Registers a listener for world changes; the modern world reports changes to its own renderer, so the listener is only kept. */
    public void addWorldAccess(IWorldAccess access) {
        worldAccesses.add(access);
    }

    public void removeWorldAccess(IWorldAccess access) {
        worldAccesses.remove(access);
    }

    private final java.util.List<IWorldAccess> worldAccesses = new java.util.concurrent.CopyOnWriteArrayList<>();

    public boolean isRaining() {
        Level level = mcc$fullLevel();
        return level != null && level.isRaining();
    }

    public boolean isThundering() {
        Level level = mcc$fullLevel();
        return level != null && level.isThundering();
    }

    public boolean canLightningStrikeAt(int x, int y, int z) {
        Level level = mcc$liveLevel();
        return level != null && level.isRainingAt(pos(x, y, z));
    }

    public float getRainStrength(float partial) {
        Level level = mcc$fullLevel();
        return level == null ? 0 : level.getRainLevel(partial);
    }

    public int getMoonPhase() {
        return lvl().getMoonPhase();
    }

    public float getCelestialAngle(float partial) {
        return lvl().getTimeOfDay(partial);
    }

    public long getSeed() {
        if (lvl() instanceof WorldGenLevel g) return g.getSeed();
        return mcc$fullLevel() instanceof ServerLevel s ? s.getSeed() : 0L;
    }

    private legacyconnector.v1_7_10.net.minecraft.world.storage.WorldInfo mcc$info;

    public legacyconnector.v1_7_10.net.minecraft.world.storage.WorldInfo getWorldInfo() {
        if (mcc$info == null) mcc$info = new legacyconnector.v1_7_10.net.minecraft.world.storage.WorldInfo(this);
        return mcc$info;
    }

    public ChunkCoordinates getSpawnPoint() {
        Level level = mcc$fullLevel();
        BlockPos spawn = level == null ? BlockPos.ZERO : level.getSharedSpawnPos();
        return new ChunkCoordinates(spawn.getX(), spawn.getY(), spawn.getZ());
    }

    public String getProviderName() {
        return mcc$get$provider(this).getDimensionName();
    }

    /** A world event every player hears wherever they are (the wither's spawn, the dragon's death). */
    public void playBroadcastSound(int type, int x, int y, int z, int data) {
        Level level = mcc$liveLevel();
        if (level != null) level.globalLevelEvent(type, pos(x, y, z), data);
    }

    /** Phantom: redirected to {@link #mcc$get$captureBlockSnapshots}. Forge's flag that block changes are being recorded for an item use. */
    @Deprecated
    public boolean captureBlockSnapshots;

    public static boolean mcc$get$captureBlockSnapshots(World w) {
        Level level = w.mcc$fullLevel();
        return level != null && level.captureBlockSnapshots;
    }

    /**
     * Legacy code turns the capture off around changes that must apply at once (bonemeal effects). Turning it on is ignored: the
     * modern snapshots would never be replayed or cleared by legacy code, which reads its own snapshot list.
     */
    public static void mcc$set$captureBlockSnapshots(World w, boolean capture) {
        Level level = w.mcc$fullLevel();
        if (level != null && !capture) level.captureBlockSnapshots = false;
    }

    // ------------------------------------------------------------------ sky, sun and stars (World asks the provider, which answers with the *Body methods)

    private static float clamp01(float f) {
        return f < 0.0F ? 0.0F : Math.min(f, 1.0F);
    }

    /** The day-light curve 1.7.10 used for the sky: cos of the sun angle, scaled and clamped to 0..1. */
    private float mcc$daylight(float partial, float offset) {
        return clamp01(net.minecraft.util.Mth.cos(getCelestialAngle(partial) * (float) Math.PI * 2.0F) * 2.0F + offset);
    }

    public legacyconnector.v1_7_10.net.minecraft.util.Vec3 getSkyColor(Entity entity, float partial) {
        return mcc$get$provider(this).getSkyColor(entity, partial);
    }

    /** Vanilla's sky colour: the biome's sky colour darkened at night and greyed by rain and thunder. */
    public legacyconnector.v1_7_10.net.minecraft.util.Vec3 getSkyColorBody(Entity entity, float partial) {
        float light = mcc$daylight(partial, 0.5F);
        int color = 0x78A7FF;
        try {
            if (entity != null) {
                BlockPos p = BlockPos.containing(entity.mcc$x(), entity.mcc$y(), entity.mcc$z());
                if (lvl().hasChunkAt(p)) color = lvl().getBiome(p).value().getSkyColor();
            }
        } catch (RuntimeException e) {
            // no biome here: the default sky
        }
        float r = (color >> 16 & 255) / 255.0F * light;
        float g = (color >> 8 & 255) / 255.0F * light;
        float b = (color & 255) / 255.0F * light;
        float rain = getRainStrength(partial);
        if (rain > 0.0F) {
            float grey = (r * 0.3F + g * 0.59F + b * 0.11F) * 0.6F;
            float keep = 1.0F - rain * 0.75F;
            r = r * keep + grey * (1.0F - keep);
            g = g * keep + grey * (1.0F - keep);
            b = b * keep + grey * (1.0F - keep);
        }
        float thunder = getWeightedThunderStrength(partial);
        if (thunder > 0.0F) {
            float grey = (r * 0.3F + g * 0.59F + b * 0.11F) * 0.2F;
            float keep = 1.0F - thunder * 0.75F;
            r = r * keep + grey * (1.0F - keep);
            g = g * keep + grey * (1.0F - keep);
            b = b * keep + grey * (1.0F - keep);
        }
        return legacyconnector.v1_7_10.net.minecraft.util.Vec3.createVectorHelper(r, g, b);
    }

    public legacyconnector.v1_7_10.net.minecraft.util.Vec3 getCloudColour(float partial) {
        return mcc$get$provider(this).drawClouds(partial);
    }

    /** Vanilla's cloud colour: white, darker at night and grey in rain and thunder. */
    public legacyconnector.v1_7_10.net.minecraft.util.Vec3 drawCloudsBody(float partial) {
        float light = mcc$daylight(partial, 0.5F);
        float r = 1.0F, g = 1.0F, b = 1.0F;
        float rain = getRainStrength(partial);
        if (rain > 0.0F) {
            float grey = (r * 0.3F + g * 0.59F + b * 0.11F) * 0.6F;
            float keep = 1.0F - rain * 0.95F;
            r = r * keep + grey * (1.0F - keep);
            g = g * keep + grey * (1.0F - keep);
            b = b * keep + grey * (1.0F - keep);
        }
        r *= light * 0.9F + 0.1F;
        g *= light * 0.9F + 0.1F;
        b *= light * 0.85F + 0.15F;
        float thunder = getWeightedThunderStrength(partial);
        if (thunder > 0.0F) {
            float grey = (r * 0.3F + g * 0.59F + b * 0.11F) * 0.2F;
            float keep = 1.0F - thunder * 0.95F;
            r = r * keep + grey * (1.0F - keep);
            g = g * keep + grey * (1.0F - keep);
            b = b * keep + grey * (1.0F - keep);
        }
        return legacyconnector.v1_7_10.net.minecraft.util.Vec3.createVectorHelper(r, g, b);
    }

    public legacyconnector.v1_7_10.net.minecraft.util.Vec3 getFogColor(float partial) {
        return mcc$get$provider(this).getFogColor(getCelestialAngle(partial), partial);
    }

    public float getStarBrightness(float partial) {
        return mcc$get$provider(this).getStarBrightness(partial);
    }

    public float getStarBrightnessBody(float partial) {
        float f = 1.0F - mcc$daylight(partial, 0.25F);
        return f * f * 0.5F;
    }

    public float getSunBrightness(float partial) {
        return mcc$get$provider(this).getSunBrightness(partial);
    }

    /** How bright the sun makes the world (0.2 at night .. 1 at noon), dimmed by rain and thunder. */
    public float getSunBrightnessBody(float partial) {
        float f = mcc$daylight(partial, 0.2F);
        f *= 1.0F - getRainStrength(partial) * 5.0F / 16.0F;
        f *= 1.0F - getWeightedThunderStrength(partial) * 5.0F / 16.0F;
        return f * 0.8F + 0.2F;
    }

    /** Forge: the raw daylight factor (0 at night .. 1 at noon) after rain and thunder. */
    public float getSunBrightnessFactor(float partial) {
        float f = mcc$daylight(partial, 0.5F);
        f = (float) (f * (1.0D - getRainStrength(partial) * 5.0F / 16.0D));
        f = (float) (f * (1.0D - getWeightedThunderStrength(partial) * 5.0F / 16.0D));
        return f;
    }

    /** How many levels the sky light is darkened by the time of day and weather (0 at noon .. 11 at night). */
    public int calculateSkylightSubtracted(float partial) {
        return (int) ((1.0F - getSunBrightnessFactor(partial)) * 11.0F);
    }

    /** The height the sky's horizon is drawn at (the provider's: 63, or 0 in flat worlds). */
    public double getHorizon() {
        return mcc$get$provider(this).getHorizon();
    }

    public void setRainStrength(float strength) {
        Level level = mcc$fullLevel();
        if (level != null) level.setRainLevel(strength);
    }

    public void setThunderStrength(float strength) {
        Level level = mcc$fullLevel();
        if (level != null) level.setThunderLevel(strength);
    }

    // ------------------------------------------------------------------ weather, snow and ice

    /** Weather runs in the modern level; a provider's {@code updateWeather} reaches this and leaves it to the level. */
    public void updateWeatherBody() {
    }

    public void updateWeather() {
        mcc$get$provider(this).updateWeather();
    }

    /** The rain and thunder strength start at the saved weather (the modern level does this when it loads). */
    public void calculateInitialWeatherBody() {
        Level level = mcc$fullLevel();
        if (level == null) return;
        level.setRainLevel(level.getLevelData().isRaining() ? 1.0F : 0.0F);
        level.setThunderLevel(level.getLevelData().isThundering() ? 1.0F : 0.0F);
    }

    public void calculateInitialWeather() {
        mcc$get$provider(this).calculateInitialWeather();
    }

    /** Sky light is computed by the modern light engine. */
    public void calculateInitialSkylight() {
    }

    public boolean canBlockFreeze(int x, int y, int z, boolean byWater) {
        return mcc$get$provider(this).canBlockFreeze(x, y, z, byWater);
    }

    /** Whether still water here turns to ice: a cold biome, dim light, and (with {@code byWater}) not surrounded by water. */
    public boolean canBlockFreezeBody(int x, int y, int z, boolean byWater) {
        try {
            BlockPos p = pos(x, y, z);
            if (!lvl().hasChunkAt(p)) return false;
            return lvl().getBiome(p).value().shouldFreeze(lvl(), p, byWater);
        } catch (RuntimeException e) {
            return false;
        }
    }

    public boolean isBlockFreezableNaturally(int x, int y, int z) {
        return canBlockFreeze(x, y, z, true);
    }

    /** Whether a snow layer may form here: a cold biome, dim light, air above a block snow can rest on. */
    public boolean canSnowAtBody(int x, int y, int z, boolean checkLight) {
        try {
            BlockPos p = pos(x, y, z);
            if (!lvl().hasChunkAt(p)) return false;
            var biome = lvl().getBiome(p).value();
            if (checkLight) return biome.shouldSnow(lvl(), p);
            return !biome.warmEnoughToRain(p) && lvl().getBlockState(p).isAir()
                    && net.minecraft.world.level.block.Blocks.SNOW.defaultBlockState().canSurvive(lvl(), p);
        } catch (RuntimeException e) {
            return false;
        }
    }

    /** Whether the biome here is humid (rainfall above 0.85: jungles and swamps). */
    public boolean isBlockHighHumidity(int x, int y, int z) {
        var biome = getBiomeGenForCoords(x, z);
        return biome != null && biome.isHighHumidity();
    }

    /** Whether the player may break the block (spawn protection and world border on a server). */
    public boolean canMineBlockBody(EntityPlayer player, int x, int y, int z) {
        Level level = mcc$liveLevel();
        if (level == null || player == null || player.mcc$player() == null) return true;
        return level.mayInteract(player.mcc$player(), pos(x, y, z));
    }

    /** Puts out the fire on the given side of the block (the player punched it), as vanilla did. */
    public boolean extinguishFire(EntityPlayer player, int x, int y, int z, int side) {
        switch (side) {
            case 0 -> y--;
            case 1 -> y++;
            case 2 -> z--;
            case 3 -> z++;
            case 4 -> x--;
            case 5 -> x++;
            default -> { }
        }
        if (getBlock(x, y, z) != legacyconnector.v1_7_10.net.minecraft.init.Blocks.fire) return false;
        playAuxSFXAtEntity(player, 1004, x, y, z, 0);
        setBlockToAir(x, y, z);
        return true;
    }

    // ------------------------------------------------------------------ light and heights

    /** The light level of the block (sky light less the time of day, or block light), as {@link #getBlockLightValue}. */
    public int getBlockLightValue_do(int x, int y, int z, boolean useNeighbours) {
        if (y < mcc$buildBottom()) return 0;
        return getBlockLightValue(x, y, z);
    }

    /** The lowest value of the chunk's height map (where sky light may reach the lowest). */
    public int getChunkHeightMapMinimum(int x, int z) {
        var chunk = lvl().getChunk(x >> 4, z >> 4, net.minecraft.world.level.chunk.status.ChunkStatus.EMPTY, false);
        if (chunk == null) return 0;
        int min = Integer.MAX_VALUE;
        for (int cx = 0; cx < 16; cx++) {
            for (int cz = 0; cz < 16; cz++) min = Math.min(min, chunk.getHeight(Heightmap.Types.MOTION_BLOCKING, cx, cz) + 1);
        }
        return legacyY(min);
    }

    /** Sky light of a column changed between two heights: the modern engine relights the blocks. */
    public void markBlocksDirtyVertical(int x, int z, int y1, int y2) {
        if (y1 > y2) {
            int t = y1;
            y1 = y2;
            y2 = t;
        }
        var engine = lvl().getLightEngine();
        if (engine != null && !mcc$get$provider(this).hasNoSky) {
            for (int y = y1; y <= y2 && y - y1 < 512; y++) engine.checkBlock(pos(x, y, z));
        }
        markBlockRangeForRenderUpdate(x, y1, z, x, y2, z);
    }

    /**
     * The local difficulty (1.7.10's {@code func_147473_B}): grows with the time players spent in the chunk and the moon phase, scaled
     * by the difficulty setting.
     */
    public float func_147473_B(int x, int y, int z) {
        var difficulty = lvl().getDifficulty();
        boolean hard = difficulty == net.minecraft.world.Difficulty.HARD;
        float f = 0.0F;
        var chunk = lvl().getChunk(x >> 4, z >> 4, net.minecraft.world.level.chunk.status.ChunkStatus.EMPTY, false);
        if (chunk != null) {
            f += clamp01(chunk.getInhabitedTime() / 3600000.0F) * (hard ? 1.0F : 0.75F);
            f += getCurrentMoonPhaseFactor() * 0.25F;
        }
        if (difficulty == net.minecraft.world.Difficulty.EASY || difficulty == net.minecraft.world.Difficulty.PEACEFUL) {
            f *= difficulty.getId() / 2.0F;
        }
        return Math.max(0.0F, Math.min(f, hard ? 1.5F : 1.0F));
    }

    // ------------------------------------------------------------------ ticks and updates the modern level runs itself

    /** The world's tick (weather and spawning) is run by the modern level. */
    public void tick() {
    }

    /** Scheduled block updates are run by the modern level; nothing is pending here. */
    public boolean tickUpdates(boolean runAll) {
        return false;
    }

    /** Entities and tile entities are ticked by the modern level. */
    public void updateEntities() {
    }

    /** A scheduled update read back from a save ({@code func_147446_b}): scheduled like any other. */
    public void func_147446_b(int x, int y, int z, Block block, int delay, int priority) {
        scheduleBlockUpdate(x, y, z, block, delay);
    }

    /** Runs one tick of the entity now (the modern level's own entity tick, on the server thread). */
    public void updateEntity(Entity entity) {
        if (entity == null || entity.mcc$entity == null || entity.mcc$entity.isRemoved()) return;
        if (!(mcc$liveLevel() instanceof ServerLevel level) || entity.mcc$entity.level() != level || !level.getServer().isSameThread()) return;
        if (entity.mcc$entity.isPassenger()) return; // ridden entities tick with their vehicle
        level.guardEntityTick(level::tickNonPassenger, entity.mcc$entity);
    }

    /** Tile entities added in bulk (a chunk's, after a move): each joins the world at its position. */
    public void func_147448_a(java.util.Collection tiles) {
        if (tiles == null) return;
        for (Object o : tiles) {
            if (o instanceof TileEntity te) addTileEntity(te);
        }
    }

    /**
     * 1.7.10 took the tile entity off the ticking list at the end of the tick, leaving it in its chunk (multiparts do this when they
     * stop needing ticks). The modern host block entity decides ticking from {@code canUpdate()}, so the tile stays as it is.
     */
    public void markTileEntityForRemoval(TileEntity te) {
    }

    /** Notifies the world listeners that static entities (paintings, item frames) changed. */
    public void func_147450_X() {
        for (IWorldAccess access : worldAccesses) {
            try {
                access.onStaticEntitiesChanged();
            } catch (RuntimeException | LinkageError e) {
                // a listener that fails is skipped
            }
        }
    }

    /** Sets the total world time ({@code func_82738_a}; the game time of the server level). */
    public void func_82738_a(long time) {
        if (mcc$fullLevel() instanceof ServerLevel s && s.getLevelData() instanceof net.minecraft.world.level.storage.ServerLevelData data) {
            data.setGameTime(time);
        }
    }

    /** Session locks belong to the modern save; there is no lock to lose here. */
    public void checkSessionLock() {
    }

    /** Disconnecting is handled by the modern client. */
    public void sendQuittingDisconnectingPacket() {
    }

    // ------------------------------------------------------------------ entities

    public List getLoadedEntityList() {
        return mcc$get$loadedEntityList(this);
    }

    public String getDebugLoadedEntities() {
        return "All: " + mcc$get$loadedEntityList(this).size();
    }

    /** Entities joining the world in bulk (moved in from another dimension or a saved area): each is spawned if not yet in it. */
    public void addLoadedEntities(List entities) {
        if (entities == null) return;
        for (Object o : new ArrayList<>(entities)) {
            if (o instanceof Entity e) joinEntityInSurroundings(e);
        }
    }

    /** Entities leaving the world without dying (moved elsewhere): removed from the level. Players are left to the server. */
    public void unloadEntities(List entities) {
        if (entities == null) return;
        for (Object o : new ArrayList<>(entities)) {
            if (o instanceof Entity e && e.mcc$entity != null && !(e.mcc$entity instanceof net.minecraft.world.entity.player.Player)) {
                e.mcc$entity.remove(net.minecraft.world.entity.Entity.RemovalReason.CHANGED_DIMENSION);
            }
        }
    }

    /** Spawns the entity unless the world already has it (vanilla also loaded the chunks around it). */
    public void joinEntityInSurroundings(Entity entity) {
        if (entity == null) return;
        if (entity.mcc$entity != null && !entity.mcc$entity.isRemoved() && entity.mcc$entity.level() == mcc$fullLevel()
                && mcc$fullLevel() instanceof ServerLevel s && s.getEntity(entity.mcc$entity.getUUID()) != null) {
            return;
        }
        spawnEntityInWorld(entity);
    }

    /** Removes the entity at once (vanilla: without the end-of-tick cleanup). Players stay: the server removes them when they leave. */
    public void removePlayerEntityDangerously(Entity entity) {
        if (entity == null) return;
        if (entity.mcc$entity != null && entity.mcc$entity instanceof net.minecraft.world.entity.player.Player) return;
        entity.setDead();
    }

    /** How many entities of the spawn category the world holds ({@code forSpawnCount}: only those that count against the mob cap). */
    public int countEntities(legacyconnector.v1_7_10.net.minecraft.entity.EnumCreatureType type, boolean forSpawnCount) {
        if (type == null || !(mcc$fullLevel() instanceof ServerLevel s)) return 0;
        var category = type.mcc$modern();
        int n = 0;
        for (var e : s.getAllEntities()) {
            if (e.getType().getCategory() != category) continue;
            if (forSpawnCount && e instanceof net.minecraft.world.entity.Mob mob && (mob.isPersistenceRequired() || mob.requiresCustomPersistence())) continue;
            n++;
        }
        return n;
    }

    /** Sets whether hostile and peaceful mobs spawn naturally in this world. */
    public void setAllowedSpawnTypes(boolean hostile, boolean peaceful) {
        Level level = mcc$fullLevel();
        if (level != null) level.setSpawnSettings(hostile, peaceful);
    }

    /** A path for the entity to another entity (to its feet), as {@link #getEntityPathToXYZ}. */
    public legacyconnector.v1_7_10.net.minecraft.pathfinding.PathEntity getPathEntityToEntity(Entity entity, Entity target, float distance,
            boolean passOpenDoors, boolean openDoors, boolean avoidWater, boolean swim) {
        if (target == null) return null;
        return getEntityPathToXYZ(entity, (int) Math.floor(target.mcc$x()), (int) Math.floor(target.mcc$y()), (int) Math.floor(target.mcc$z()), distance,
                passOpenDoors, openDoors, avoidWater, swim);
    }

    /** Whether any block that is not air overlaps the box. */
    public boolean checkBlockCollision(AxisAlignedBB box) {
        int x1 = (int) Math.floor(box.minX), x2 = (int) Math.floor(box.maxX + 1.0D);
        int y1 = (int) Math.floor(box.minY), y2 = (int) Math.floor(box.maxY + 1.0D);
        int z1 = (int) Math.floor(box.minZ), z2 = (int) Math.floor(box.maxZ + 1.0D);
        if (box.minX < 0.0D) x1--;
        if (box.minY < 0.0D) y1--;
        if (box.minZ < 0.0D) z1--;
        for (int x = x1; x < x2; x++) {
            for (int y = y1; y < y2; y++) {
                for (int z = z1; z < z2; z++) {
                    Block block = getBlock(x, y, z);
                    if (block != null && block.getMaterial() != legacyconnector.v1_7_10.net.minecraft.block.material.Material.air) return true;
                }
            }
        }
        return false;
    }

    /** The world's crash report section: its name, chunk statistics and level data. */
    public legacyconnector.v1_7_10.net.minecraft.crash.CrashReportCategory addWorldInfoToCrashReport(legacyconnector.v1_7_10.net.minecraft.crash.CrashReport report) {
        var category = report.makeCategoryDepth("Affected level", 1);
        category.addCrashSection("Level name", getWorldInfo().getWorldName());
        category.addCrashSectionCallable("All players", () -> mcc$get$playerEntities(this).size() + " total");
        category.addCrashSectionCallable("Chunk stats", () -> getChunkProvider().makeString());
        category.addCrashSection("Level", String.valueOf(lvl()));
        return category;
    }

    // ------------------------------------------------------------------ spawn and structures

    /** Sets the world spawn to the first surface block from (8, 64, 8) (vanilla's default spawn search). */
    public void setSpawnLocation() {
        setSpawnLocation(8, 64, 8);
    }

    public void setSpawnLocation(int x, int y, int z) {
        if (mcc$fullLevel() instanceof ServerLevel s) s.setDefaultSpawnPos(new BlockPos(x, y, z), 0.0F);
    }

    /**
     * The nearest structure of a legacy name ("Stronghold", "Village", "Mineshaft", "Temple", "Fortress", "Monument"): the modern
     * structure locator (what eyes of ender use), within 100 chunks. Null when there is none or this is not the server thread.
     */
    public ChunkPosition findClosestStructure(String name, int x, int y, int z) {
        if (name == null || !(mcc$liveLevel() instanceof ServerLevel level) || !level.getServer().isSameThread()) return null;
        String[] ids = switch (name) {
            case "Stronghold" -> new String[] {"stronghold"};
            case "Village" -> new String[] {"village_plains", "village_desert", "village_savanna", "village_snowy", "village_taiga"};
            case "Mineshaft" -> new String[] {"mineshaft", "mineshaft_mesa"};
            case "Temple" -> new String[] {"desert_pyramid", "jungle_pyramid", "swamp_hut", "igloo"};
            case "Fortress" -> new String[] {"fortress"};
            case "Monument" -> new String[] {"monument"};
            default -> new String[0];
        };
        try {
            var registry = level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.STRUCTURE);
            List<net.minecraft.core.Holder<net.minecraft.world.level.levelgen.structure.Structure>> holders = new ArrayList<>();
            for (String id : ids) {
                registry.getHolder(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.STRUCTURE,
                        net.minecraft.resources.ResourceLocation.withDefaultNamespace(id))).ifPresent(holders::add);
            }
            if (holders.isEmpty()) return null;
            var found = level.getChunkSource().getGenerator().findNearestMapStructure(level, net.minecraft.core.HolderSet.direct(holders), pos(x, y, z), 100, false);
            if (found == null) return null;
            BlockPos at = found.getFirst();
            return new ChunkPosition(at.getX(), legacyY(at.getY()), at.getZ());
        } catch (RuntimeException e) {
            return null;
        }
    }

    // ------------------------------------------------------------------ helpers used by other shims

    public int mcc$maxBuildHeight() {
        return lvl().getMaxBuildHeight();
    }

    public void mcc$playPlaceSound(Block block, int x, int y, int z) {
        playSoundEffect(x + 0.5, y + 0.5, z + 0.5, block.stepSound.getPlaceSound(), (block.stepSound.getVolume() + 1.0F) / 2.0F,
                block.stepSound.getPitch() * 0.8F);
    }

    public void mcc$dropXp(int x, int y, int z, int amount) {
        if (mcc$liveLevel() instanceof ServerLevel s && amount > 0) {
            net.minecraft.world.entity.ExperienceOrb.award(s, net.minecraft.world.phys.Vec3.atCenterOf(pos(x, y, z)), amount);
        }
    }

    @Override
    public String toString() {
        return "LegacyWorld{" + lvl() + "}";
    }

    /** Forge: chunks held loaded by chunk-loading tickets. */
    public com.google.common.collect.ImmutableSetMultimap<ChunkCoordIntPair, legacyconnector.v1_7_10.net.minecraftforge.common.ForgeChunkManager.Ticket> getPersistentChunks() {
        return legacyconnector.v1_7_10.net.minecraftforge.common.ForgeChunkManager.getPersistentChunksFor(this);
    }

    public GameRules getGameRules() {
        return GameRules.mcc$of(mcc$fullLevel());
    }

    /** Whether any block overlapping the box has the given material (vanilla: water checks of items, entities). */
    public boolean isMaterialInBB(legacyconnector.v1_7_10.net.minecraft.util.AxisAlignedBB box,
            legacyconnector.v1_7_10.net.minecraft.block.material.Material material) {
        int x1 = (int) Math.floor(box.minX), x2 = (int) Math.floor(box.maxX + 1.0);
        int y1 = (int) Math.floor(box.minY), y2 = (int) Math.floor(box.maxY + 1.0);
        int z1 = (int) Math.floor(box.minZ), z2 = (int) Math.floor(box.maxZ + 1.0);
        for (int x = x1; x < x2; x++) {
            for (int y = y1; y < y2; y++) {
                for (int z = z1; z < z2; z++) {
                    var block = getBlock(x, y, z);
                    if (block != null && block.getMaterial() == material) return true;
                }
            }
        }
        return false;
    }

    /** Whether any block overlapping the box is a liquid. */
    public boolean isAnyLiquid(legacyconnector.v1_7_10.net.minecraft.util.AxisAlignedBB box) {
        int x1 = (int) Math.floor(box.minX), x2 = (int) Math.floor(box.maxX + 1.0);
        int y1 = (int) Math.floor(box.minY), y2 = (int) Math.floor(box.maxY + 1.0);
        int z1 = (int) Math.floor(box.minZ), z2 = (int) Math.floor(box.maxZ + 1.0);
        for (int x = x1; x < x2; x++) {
            for (int y = Math.max(0, y1); y < y2; y++) {
                for (int z = z1; z < z2; z++) {
                    var block = getBlock(x, y, z);
                    if (block != null && block.getMaterial().isLiquid()) return true;
                }
            }
        }
        return false;
    }

    /** The calendar date (vanilla: the real clock; mods time-limit features by it). */
    public java.util.Calendar getCurrentDate() {
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        return calendar;
    }

    /** Notifies the neighbours of a block that its state changed (comparators and redstone read this). */
    public void updateNeighborsAboutBlockChange(int x, int y, int z, Block block) {
        notifyBlocksOfNeighborChange(x, y, z, block);
    }

    public boolean restoringBlockSnapshots;

    public boolean updateLightByType(EnumSkyBlock type, int x, int y, int z) {
        LevelAccessor level = lvl();
        if (level.getLightEngine() != null) level.getLightEngine().checkBlock(pos(x, y, z));
        return true;
    }

    public void setEntityState(Entity entity, byte state) {
        if (entity.mcc$entity != null && entity.mcc$entity.level() instanceof net.minecraft.server.level.ServerLevel server) {
            server.broadcastEntityEvent(entity.mcc$entity, state);
        }
    }

    /** Phantom: redirected to {@link #mcc$get$weatherEffects}. Mods add lightning to it directly (Mystcraft's lightning effect). */
    @Deprecated
    public List weatherEffects;
    private List mcc$weatherEffects;

    /** A list whose additions strike: 1.7.10 ticked and sent every entity in it. */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static List mcc$get$weatherEffects(World w) {
        if (w.mcc$weatherEffects == null) {
            w.mcc$weatherEffects = new java.util.ArrayList() {
                @Override
                public boolean add(Object o) {
                    if (o instanceof Entity e) w.addWeatherEffect(e);
                    return true;
                }
            };
        }
        return w.mcc$weatherEffects;
    }

    /** Lightning a mod creates strikes as a modern lightning bolt (fire, damage, sound and flash included). */
    public boolean addWeatherEffect(Entity entity) {
        if (!(entity instanceof legacyconnector.v1_7_10.net.minecraft.entity.effect.EntityLightningBolt)
                || !(mcc$liveLevel() instanceof net.minecraft.server.level.ServerLevel level)) {
            return false;
        }
        var bolt = net.minecraft.world.entity.EntityType.LIGHTNING_BOLT.create(level);
        if (bolt == null) return false;
        bolt.moveTo(Entity.mcc$get$posX(entity), Entity.mcc$get$posY(entity), Entity.mcc$get$posZ(entity));
        return level.addFreshEntity(bolt);
    }

    public void updateEntityWithOptionalForce(Entity entity, boolean force) {
    }

    public void updateAllPlayersSleepingFlag() {
    }

    public void onEntityRemoved(Entity entity) {
    }

    /** An entity joined the world: 1.7.10 told its world listeners; the modern level tracks entities itself (see {@link IWorldAccess}). */
    public void onEntityAdded(Entity entity) {
    }

    /**
     * Block-change notifications after a block was set ({@code setBlock} called this in Forge): clients with flag 2 (unless
     * flag 4 on a client world), neighbours with flag 1, and comparators reading the new block.
     */
    public void markAndNotifyBlock(int x, int y, int z, legacyconnector.v1_7_10.net.minecraft.world.chunk.Chunk chunk, Block oldBlock, Block newBlock,
            int flags) {
        if ((flags & 2) != 0 && (!isRemote || (flags & 4) == 0)) markBlockForUpdate(x, y, z);
        if (!isRemote && (flags & 1) != 0) {
            notifyBlockChange(x, y, z, oldBlock);
            if (newBlock != null && newBlock.hasComparatorInputOverride()) func_147453_f(x, y, z, newBlock);
        }
    }

    /**
     * A path for the entity to the block, at most {@code distance} blocks long, found by a ground navigator with the given door
     * and swimming abilities (water avoidance is the entity's own). Null for entities that cannot path (not mobs) or on clients.
     */
    public legacyconnector.v1_7_10.net.minecraft.pathfinding.PathEntity getEntityPathToXYZ(Entity entity, int x, int y, int z, float distance,
            boolean passOpenDoors, boolean openDoors, boolean avoidWater, boolean swim) {
        if (entity == null || !(entity.mcc$entity instanceof net.minecraft.world.entity.Mob mob) || !(mob.level() instanceof ServerLevel level)) return null;
        var navigation = new net.minecraft.world.entity.ai.navigation.GroundPathNavigation(mob, level) {
            net.minecraft.world.level.pathfinder.Path mcc$find(BlockPos target, float range) {
                return createPath(java.util.Set.of(target), 8, false, 0, range);
            }
        };
        navigation.setCanPassDoors(passOpenDoors);
        navigation.setCanOpenDoors(openDoors);
        navigation.setCanFloat(swim);
        var path = navigation.mcc$find(new BlockPos(x, y, z), distance);
        return path == null ? null : new legacyconnector.v1_7_10.net.minecraft.pathfinding.PathEntity(path);
    }

    /**
     * Pushes the entity along the flow of the liquid of {@code material} it overlaps (vanilla: water currents); true if it
     * touches any. Vanilla liquids flow as the modern fluid does, mod liquids by their {@code velocityToAddToEntity}.
     */
    public boolean handleMaterialAcceleration(AxisAlignedBB box, legacyconnector.v1_7_10.net.minecraft.block.material.Material material, Entity entity) {
        int x1 = (int) Math.floor(box.minX), x2 = (int) Math.floor(box.maxX + 1.0D);
        int y1 = (int) Math.floor(box.minY), y2 = (int) Math.floor(box.maxY + 1.0D);
        int z1 = (int) Math.floor(box.minZ), z2 = (int) Math.floor(box.maxZ + 1.0D);
        if (!checkChunksExist(x1, y1, z1, x2, y2, z2)) return false;
        boolean inside = false;
        legacyconnector.v1_7_10.net.minecraft.util.Vec3 flow = legacyconnector.v1_7_10.net.minecraft.util.Vec3.createVectorHelper(0.0D, 0.0D, 0.0D);
        for (int x = x1; x < x2; x++) {
            for (int y = y1; y < y2; y++) {
                for (int z = z1; z < z2; z++) {
                    Block block = getBlock(x, y, z);
                    if (block == null || block.getMaterial() != material) continue;
                    double surface = y + 1 - legacyconnector.v1_7_10.net.minecraft.block.BlockLiquid.getLiquidHeightPercent(getBlockMetadata(x, y, z));
                    if (y2 < surface) continue;
                    inside = true;
                    if (block.mcc$vanilla != null) {
                        BlockPos p = pos(x, y, z);
                        var fluid = lvl().getFluidState(p);
                        if (!fluid.isEmpty()) {
                            var v = fluid.getFlow(lvl(), p);
                            flow.xCoord += v.x;
                            flow.yCoord += v.y;
                            flow.zCoord += v.z;
                        }
                    } else {
                        block.velocityToAddToEntity(this, x, y, z, entity, flow);
                    }
                }
            }
        }
        boolean pushed = entity == null || entity.mcc$entity == null || entity.mcc$entity.isPushedByFluid();
        if (flow.lengthVector() > 0.0D && pushed && entity != null) {
            flow = flow.normalize();
            Entity.mcc$set$motionX(entity, Entity.mcc$get$motionX(entity) + flow.xCoord * 0.014D);
            Entity.mcc$set$motionY(entity, Entity.mcc$get$motionY(entity) + flow.yCoord * 0.014D);
            Entity.mcc$set$motionZ(entity, Entity.mcc$get$motionZ(entity) + flow.zCoord * 0.014D);
        }
        return inside;
    }

    public void addTileEntity(legacyconnector.v1_7_10.net.minecraft.tileentity.TileEntity tile) {
    }

    public int getBlockLightOpacity(int x, int y, int z) {
        return getBlock(x, y, z).getLightOpacity(this, x, y, z);
    }

    public float getCelestialAngleRadians(float partial) {
        return getCelestialAngle(partial) * (float) Math.PI * 2.0F;
    }

    public boolean isAABBInMaterial(AxisAlignedBB box, legacyconnector.v1_7_10.net.minecraft.block.material.Material material) {
        int x0 = (int) Math.floor(box.minX);
        int x1 = (int) Math.floor(box.maxX + 1.0D);
        int y0 = (int) Math.floor(box.minY);
        int y1 = (int) Math.floor(box.maxY + 1.0D);
        int z0 = (int) Math.floor(box.minZ);
        int z1 = (int) Math.floor(box.maxZ + 1.0D);
        for (int x = x0; x < x1; x++) {
            for (int y = y0; y < y1; y++) {
                for (int z = z0; z < z1; z++) {
                    if (getBlock(x, y, z).getMaterial() == material) return true;
                }
            }
        }
        return false;
    }

    public java.util.List func_147461_a(AxisAlignedBB box) {
        return getCollidingBoundingBoxes(null, box);
    }

    /** The scheduled block updates of the chunk (and two blocks around it), optionally removing them; null when there are none. */
    public java.util.List getPendingBlockUpdates(legacyconnector.v1_7_10.net.minecraft.world.chunk.Chunk chunk, boolean remove) {
        if (chunk == null) return null;
        int x1 = (chunk.xPosition << 4) - 2, z1 = (chunk.zPosition << 4) - 2;
        // the whole height of the column: modern worlds reach below 0 and above 255
        return func_147491_a(new legacyconnector.v1_7_10.net.minecraft.world.gen.structure.StructureBoundingBox(x1, -4096, z1, x1 + 18, 4096, z1 + 18), remove);
    }

    /**
     * The scheduled block updates inside the box (x and z exclusive of the maximum), as {@link NextTickListEntry}s with their due time and
     * priority; with {@code remove} they are taken off the schedule (a mod moving the blocks schedules them again). Null when there are none.
     */
    public java.util.List func_147491_a(legacyconnector.v1_7_10.net.minecraft.world.gen.structure.StructureBoundingBox box, boolean remove) {
        if (box == null || !(mcc$liveLevel() instanceof ServerLevel level) || !level.getServer().isSameThread()) return null;
        java.util.List<NextTickListEntry> out = new ArrayList<>();
        for (int cx = box.minX >> 4; cx <= (box.maxX - 1) >> 4; cx++) {
            for (int cz = box.minZ >> 4; cz <= (box.maxZ - 1) >> 4; cz++) {
                var chunk = level.getChunkSource().getChunkNow(cx, cz);
                if (chunk == null || !(chunk.getBlockTicks() instanceof net.minecraft.world.ticks.LevelChunkTicks<net.minecraft.world.level.block.Block> ticks)) continue;
                ticks.getAll().forEach(t -> {
                    BlockPos p = t.pos();
                    if (p.getX() < box.minX || p.getX() >= box.maxX || p.getZ() < box.minZ || p.getZ() >= box.maxZ) return;
                    if (p.getY() < box.minY || p.getY() > box.maxY) return;
                    var entry = new NextTickListEntry(p.getX(), p.getY(), p.getZ(), BlockBridge.legacyBlock(t.type().defaultBlockState()));
                    entry.setScheduledTime(t.triggerTick());
                    entry.setPriority(t.priority().getValue());
                    out.add(entry);
                });
            }
        }
        if (out.isEmpty()) return null;
        if (remove) {
            level.getBlockTicks().clearArea(new net.minecraft.world.level.levelgen.structure.BoundingBox(box.minX, Math.max(level.getMinBuildHeight(), box.minY),
                    box.minZ, box.maxX - 1, Math.min(level.getMaxBuildHeight(), box.maxY), box.maxZ - 1));
        }
        return out;
    }

    /** Whether snow may settle here; the dimension's provider decides (by default {@link #canSnowAtBody}). */
    public boolean canSnowAt(int x, int y, int z, boolean checkLight) {
        return mcc$get$provider(this).canSnowAt(x, y, z, checkLight);
    }

    /** Whether still water here freezes (any water, not only at a shore). */
    public boolean isBlockFreezable(int x, int y, int z) {
        return canBlockFreeze(x, y, z, false);
    }

    public int getSkyBlockTypeBrightness(EnumSkyBlock type, int x, int y, int z) {
        return lvl().getBrightness(type == EnumSkyBlock.Sky ? net.minecraft.world.level.LightLayer.SKY : net.minecraft.world.level.LightLayer.BLOCK, pos(x, y, z));
    }

    public void destroyBlockInWorldPartially(int breakerId, int x, int y, int z, int progress) {
    }

    public void playRecord(String name, int x, int y, int z) {
    }

    /** How full the moon is (1 full .. 0 new), vanilla's per-phase table. */
    public float getCurrentMoonPhaseFactor() {
        return net.minecraft.world.level.dimension.DimensionType.MOON_BRIGHTNESS_PER_PHASE[Math.floorMod(getMoonPhase(), 8)];
    }

    public float getBlockDensity(legacyconnector.v1_7_10.net.minecraft.util.Vec3 origin, AxisAlignedBB box) {
        return 1.0F;
    }

    public void makeFireworks(double x, double y, double z, double motionX, double motionY, double motionZ, legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound tag) {
    }

    /** Whether any fluid block is inside the box (vanilla's func_147470_e). */
    public boolean func_147470_e(legacyconnector.v1_7_10.net.minecraft.util.AxisAlignedBB box) {
        return isAnyLiquid(box);
    }

    /** Sets the light level stored for a block position; the modern engine computes light itself, so this does nothing. */
    public void setLightValue(EnumSkyBlock type, int x, int y, int z, int value) {
    }

    /** The local difficulty at the point ({@link #func_147473_B} of its block). */
    public float getTensionFactorForBlock(double x, double y, double z) {
        return func_147473_B((int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z));
    }
}
