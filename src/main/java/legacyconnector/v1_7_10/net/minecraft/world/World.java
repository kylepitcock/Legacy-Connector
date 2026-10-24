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
        this.isRemote = level.isClientSide();
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

    public static World mcc$wrap(LevelAccessor level) {
        if (level == null) return null;
        return VIEWS.computeIfAbsent(level, l -> l instanceof net.minecraft.server.level.ServerLevel
                || l instanceof net.minecraft.world.level.WorldGenLevel ? new WorldServer(l)
                : l.isClientSide() ? new legacyconnector.v1_7_10.net.minecraft.client.multiplayer.WorldClient(l) : new World(l));
    }

    public LevelAccessor mcc$level() {
        return lvl();
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

    public static List mcc$get$loadedEntityList(World w) {
        List<Entity> out = new ArrayList<>();
        Level level = w.mcc$fullLevel();
        if (level instanceof ServerLevel s) for (var e : s.getAllEntities()) out.add(EntityBridge.wrap(e));
        return out;
    }

    public static List mcc$get$loadedTileEntityList(World w) {
        return new ArrayList<>();
    }

    // ------------------------------------------------------------------ blocks

    @Override
    public Block getBlock(int x, int y, int z) {
        BlockState state = lvl().getBlockState(pos(x, y, z));
        // Ore generators replace stone; the deep layer is deepslate.
        if (state.is(Blocks.DEEPSLATE) && inDeepPass()) return legacyconnector.v1_7_10.net.minecraft.init.Blocks.stone;
        return BlockBridge.legacyBlock(state);
    }

    @Override
    public int getBlockMetadata(int x, int y, int z) {
        return BlockBridge.legacyMeta(lvl().getBlockState(pos(x, y, z)));
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

    public boolean setBlock(int x, int y, int z, Block block, int meta, int flags) {
        BlockState state = BlockBridge.toModern(block, meta);
        BlockPos p = pos(x, y, z);
        if (outsideRegion(p)) return false;
        TileEntityBridge.beforeBlockChange(this, p);
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
        return lvl().getBlockState(pos(x, y, z)).isAir();
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

    public boolean canMineBlock(EntityPlayer player, int x, int y, int z) {
        return true;
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

    @Override
    public int getHeight() {
        return 256;
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
        lvl().scheduleTick(pos(x, y, z), BlockBridge.modernBlock(block), delay);
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
        return EntityBridge.spawn(this, entity);
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
        int y = lvl().hasChunk(x >> 4, z >> 4)
                ? lvl().getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE_WG, x, z)
                : lvl().getSeaLevel();
        net.minecraft.core.Holder<net.minecraft.world.level.biome.Biome> holder;
        try {
            holder = lvl().getBiome(pos(x, y, z));
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

    /** A view of the chunk; null when it is not available here (outside a generation region, or not loaded). */
    public legacyconnector.v1_7_10.net.minecraft.world.chunk.Chunk getChunkFromChunkCoords(int cx, int cz) {
        var access = lvl().getChunk(cx, cz, net.minecraft.world.level.chunk.status.ChunkStatus.EMPTY, false);
        return access == null ? null : new legacyconnector.v1_7_10.net.minecraft.world.chunk.Chunk(this, access);
    }

    public boolean chunkExists(int cx, int cz) {
        return lvl().hasChunk(cx, cz);
    }

    /** The world's chunks as a 1.7.10 chunk provider (only chunks that are loaded or generating here are provided). */
    public legacyconnector.v1_7_10.net.minecraft.world.chunk.IChunkProvider getChunkProvider() {
        World world = this;
        return new legacyconnector.v1_7_10.net.minecraft.world.chunk.IChunkProvider() {
            @Override
            public boolean chunkExists(int x, int z) {
                return world.chunkExists(x, z);
            }

            @Override
            public legacyconnector.v1_7_10.net.minecraft.world.chunk.Chunk provideChunk(int x, int z) {
                return world.getChunkFromChunkCoords(x, z);
            }

            @Override
            public String makeString() {
                return "ServerChunkCache";
            }

            @Override
            public int getLoadedChunkCount() {
                return world.mcc$fullLevel() instanceof net.minecraft.server.level.ServerLevel s ? s.getChunkSource().getLoadedChunksCount() : 0;
            }
        };
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

    /** Null on a miss, as in 1.7.10. The modern clip stands in for the block-by-block walk over each block's collisionRayTrace. */
    public legacyconnector.v1_7_10.net.minecraft.util.MovingObjectPosition rayTraceBlocks(legacyconnector.v1_7_10.net.minecraft.util.Vec3 start,
            legacyconnector.v1_7_10.net.minecraft.util.Vec3 end, boolean liquids, boolean ignoreNoBoundingBox, boolean returnLastUncollidable) {
        var hit = lvl().clip(new net.minecraft.world.level.ClipContext(start.mcc$modern(), end.mcc$modern(),
                ignoreNoBoundingBox ? net.minecraft.world.level.ClipContext.Block.OUTLINE : net.minecraft.world.level.ClipContext.Block.COLLIDER,
                liquids ? net.minecraft.world.level.ClipContext.Fluid.ANY : net.minecraft.world.level.ClipContext.Fluid.NONE,
                net.minecraft.world.phys.shapes.CollisionContext.empty()));
        if (hit.getType() == net.minecraft.world.phys.HitResult.Type.MISS) return null;
        var pos = hit.getBlockPos();
        var at = hit.getLocation();
        return new legacyconnector.v1_7_10.net.minecraft.util.MovingObjectPosition(pos.getX(), pos.getY(), pos.getZ(), hit.getDirection().get3DDataValue(),
                legacyconnector.v1_7_10.net.minecraft.util.Vec3.createVectorHelper(at.x, at.y, at.z));
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
        for (var e : lvl().getEntities((net.minecraft.world.entity.Entity) null, modern, e -> true)) {
            Entity legacy = EntityBridge.wrap(e);
            if (type.isInstance(legacy) && (selector == null || selector.isEntityApplicable(legacy))) out.add(legacy);
        }
        return out;
    }

    public List getEntitiesWithinAABBExcludingEntity(Entity except, AxisAlignedBB box) {
        List<Entity> out = new ArrayList<>();
        var exceptModern = except == null ? null : except.mcc$entity();
        for (var e : lvl().getEntities(exceptModern, box.mcc$modern(), e -> true)) out.add(EntityBridge.wrap(e));
        return out;
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

    public void playAuxSFX(int type, int x, int y, int z, int data) {
        lvl().levelEvent(type, pos(x, y, z), data);
    }

    public void playAuxSFXAtEntity(EntityPlayer player, int type, int x, int y, int z, int data) {
        lvl().levelEvent(player == null ? null : player.mcc$player(), type, pos(x, y, z), data);
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
}
