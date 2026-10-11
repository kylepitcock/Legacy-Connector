package mccompat.v1_7_10.net.minecraft.world;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.WeakHashMap;
import mccompat.v1_7_10.net.minecraft.block.Block;
import mccompat.v1_7_10.net.minecraft.entity.Entity;
import mccompat.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import mccompat.v1_7_10.net.minecraft.item.ItemStack;
import mccompat.v1_7_10.net.minecraft.tileentity.TileEntity;
import mccompat.v1_7_10.net.minecraft.util.AxisAlignedBB;
import mccompat.v1_7_10.net.minecraft.util.ChunkCoordinates;
import mccompat.v1_7_10.net.minecraftforge.common.util.ForgeDirection;
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
import org.kylepitcock.mccompat.v1_7_10.bridge.BlockBridge;
import org.kylepitcock.mccompat.v1_7_10.bridge.EntityBridge;
import org.kylepitcock.mccompat.v1_7_10.bridge.SoundBridge;
import org.kylepitcock.mccompat.v1_7_10.bridge.TileEntityBridge;

/**
 * The 1.7.10 World API as a view over a modern {@link LevelAccessor}: a live {@link Level} during gameplay, or a
 * {@code WorldGenRegion} while legacy world generators run. Coordinates are absolute in both versions, so legacy
 * code keeps working even though worlds now span y=-64..319.
 */
public class World implements IBlockAccess {
    private static final Map<LevelAccessor, World> VIEWS = Collections.synchronizedMap(new WeakHashMap<>());

    public final boolean isRemote;
    public Random rand = new Random();
    public final WorldProvider provider;
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
    public mccompat.v1_7_10.net.minecraft.world.storage.MapStorage mapStorage;
    /** Phantom: redirected to {@link #mcc$get$perWorldStorage}. Saved data of this dimension. */
    @Deprecated
    public mccompat.v1_7_10.net.minecraft.world.storage.MapStorage perWorldStorage;

    private static final Map<Object, mccompat.v1_7_10.net.minecraft.world.storage.MapStorage> STORAGES =
            Collections.synchronizedMap(new WeakHashMap<>());
    private static final mccompat.v1_7_10.net.minecraft.world.storage.MapStorage CLIENT_STORAGE =
            new mccompat.v1_7_10.net.minecraft.world.storage.MapStorage(null);

    private static mccompat.v1_7_10.net.minecraft.world.storage.MapStorage storage(ServerLevel level) {
        if (level == null) return CLIENT_STORAGE;
        var data = level.getDataStorage();
        return STORAGES.computeIfAbsent(data, d -> new mccompat.v1_7_10.net.minecraft.world.storage.MapStorage(data));
    }

    public static mccompat.v1_7_10.net.minecraft.world.storage.MapStorage mcc$get$mapStorage(World w) {
        return storage(w.mcc$fullLevel() instanceof ServerLevel s ? s.getServer().overworld() : null);
    }

    public static mccompat.v1_7_10.net.minecraft.world.storage.MapStorage mcc$get$perWorldStorage(World w) {
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
        Level full = mcc$fullLevel(level);
        this.provider = full == null ? new WorldProvider(Level.OVERWORLD, true, false)
                : new WorldProvider(full.dimension(), full.dimensionType().hasSkyLight(), full.dimensionType().ultraWarm());
    }

    public static World mcc$wrap(LevelAccessor level) {
        if (level == null) return null;
        return VIEWS.computeIfAbsent(level, l -> l instanceof net.minecraft.server.level.ServerLevel
                || l instanceof net.minecraft.world.level.WorldGenLevel ? new WorldServer(l)
                : l.isClientSide() ? new mccompat.v1_7_10.net.minecraft.client.multiplayer.WorldClient(l) : new World(l));
    }

    public LevelAccessor mcc$level() {
        return mcc$level;
    }

    /** The full level, or null for a detached world-gen region without one. */
    public Level mcc$fullLevel() {
        return mcc$fullLevel(mcc$level);
    }

    private static Level mcc$fullLevel(LevelAccessor level) {
        if (level instanceof Level l) return l;
        if (level instanceof WorldGenLevel g) return g.getLevel();
        return null;
    }

    protected static BlockPos pos(int x, int y, int z) {
        return new BlockPos(x, y, z);
    }

    // ------------------------------------------------------------------ virtual fields

    public static List mcc$get$playerEntities(World w) {
        List<EntityPlayer> out = new ArrayList<>();
        for (var p : w.mcc$level.players()) out.add((EntityPlayer) EntityBridge.wrap(p));
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
        return BlockBridge.legacyBlock(mcc$level.getBlockState(pos(x, y, z)));
    }

    @Override
    public int getBlockMetadata(int x, int y, int z) {
        return BlockBridge.legacyMeta(mcc$level.getBlockState(pos(x, y, z)));
    }

    public boolean setBlock(int x, int y, int z, Block block, int meta, int flags) {
        BlockState state = BlockBridge.toModern(block, meta);
        BlockPos p = pos(x, y, z);
        TileEntityBridge.beforeBlockChange(this, p);
        return mcc$level.setBlock(p, state, flags);
    }

    public boolean setBlock(int x, int y, int z, Block block) {
        return setBlock(x, y, z, block, 0, 3);
    }

    public boolean setBlockMetadataWithNotify(int x, int y, int z, int meta, int flags) {
        BlockPos p = pos(x, y, z);
        BlockState current = mcc$level.getBlockState(p);
        BlockState next = BlockBridge.withMeta(current, meta);
        if (next == current) return false;
        return mcc$level.setBlock(p, next, flags);
    }

    public boolean setBlockToAir(int x, int y, int z) {
        TileEntityBridge.beforeBlockChange(this, pos(x, y, z));
        return mcc$level.setBlock(pos(x, y, z), Blocks.AIR.defaultBlockState(), 3);
    }

    /** {@code func_147480_a}: break a block, optionally dropping its items. */
    public boolean breakBlock(int x, int y, int z, boolean drop) {
        TileEntityBridge.beforeBlockChange(this, pos(x, y, z));
        return mcc$level.destroyBlock(pos(x, y, z), drop);
    }

    @Override
    public boolean isAirBlock(int x, int y, int z) {
        return mcc$level.getBlockState(pos(x, y, z)).isAir();
    }

    public boolean blockExists(int x, int y, int z) {
        return mcc$level.hasChunkAt(pos(x, y, z));
    }

    public boolean checkChunksExist(int x1, int y1, int z1, int x2, int y2, int z2) {
        return mcc$level.hasChunksAt(x1, z1, x2, z2);
    }

    public boolean doChunksNearChunkExist(int x, int y, int z, int range) {
        return checkChunksExist(x - range, y - range, z - range, x + range, y + range, z + range);
    }

    public boolean isBlockNormalCubeDefault(int x, int y, int z, boolean def) {
        BlockPos p = pos(x, y, z);
        if (!mcc$level.hasChunkAt(p)) return def;
        return mcc$level.getBlockState(p).isRedstoneConductor(mcc$level, p);
    }

    public boolean isBlockFullCube(int x, int y, int z) {
        BlockPos p = pos(x, y, z);
        return mcc$level.getBlockState(p).isCollisionShapeFullBlock(mcc$level, p);
    }

    @Override
    public boolean isSideSolid(int x, int y, int z, ForgeDirection side, boolean defaultValue) {
        BlockPos p = pos(x, y, z);
        if (!mcc$level.hasChunkAt(p)) return defaultValue;
        Direction d = side.mcc$modern();
        return d != null && mcc$level.getBlockState(p).isFaceSturdy(mcc$level, p, d);
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

    public int getHeightValue(int x, int z) {
        return mcc$level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
    }

    public int getTopSolidOrLiquidBlock(int x, int z) {
        return mcc$level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
    }

    public int getPrecipitationHeight(int x, int z) {
        return mcc$level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
    }

    public boolean canBlockSeeTheSky(int x, int y, int z) {
        return mcc$level.canSeeSky(pos(x, y, z));
    }

    /** Brightness 0..1 (light level scaled by the dimension's ambient light curve). */
    public float getLightBrightness(int x, int y, int z) {
        return mcc$level.getLightLevelDependentMagicValue(pos(x, y, z));
    }

    public int getBlockLightValue(int x, int y, int z) {
        return mcc$level.getMaxLocalRawBrightness(pos(x, y, z));
    }

    public int getFullBlockLightValue(int x, int y, int z) {
        return getBlockLightValue(x, y, z);
    }

    @Override
    public int getLightBrightnessForSkyBlocks(int x, int y, int z, int min) {
        BlockPos p = pos(x, y, z);
        int sky = mcc$level.getBrightness(net.minecraft.world.level.LightLayer.SKY, p);
        int block = Math.max(min, mcc$level.getBrightness(net.minecraft.world.level.LightLayer.BLOCK, p));
        return sky << 20 | block << 4;
    }

    @Override
    public int getHeight() {
        return 256;
    }

    public int getActualHeight() {
        return provider.getActualHeight();
    }

    @Override
    public boolean extendedLevelsInChunkCache() {
        return false;
    }

    // ------------------------------------------------------------------ updates and redstone

    public void markBlockForUpdate(int x, int y, int z) {
        Level level = mcc$fullLevel();
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
        Level level = mcc$fullLevel();
        if (level != null) level.blockEntityChanged(pos(x, y, z));
    }

    public void notifyBlocksOfNeighborChange(int x, int y, int z, Block block) {
        mcc$level.blockUpdated(pos(x, y, z), BlockBridge.modernBlock(block));
    }

    public void notifyBlocksOfNeighborChange(int x, int y, int z, Block block, int skipSide) {
        Level level = mcc$fullLevel();
        if (level == null) return;
        Direction skip = skipSide >= 0 && skipSide < 6 ? Direction.from3DDataValue(skipSide) : null;
        level.updateNeighborsAtExceptFromFacing(pos(x, y, z), BlockBridge.modernBlock(block), skip);
    }

    public void notifyBlockOfNeighborChange(int x, int y, int z, Block block) {
        Level level = mcc$fullLevel();
        if (level != null) level.neighborChanged(pos(x, y, z), BlockBridge.modernBlock(block), pos(x, y, z));
    }

    public void notifyBlockChange(int x, int y, int z, Block block) {
        notifyBlocksOfNeighborChange(x, y, z, block);
    }

    public void func_147453_f(int x, int y, int z, Block block) {
        Level level = mcc$fullLevel();
        if (level != null) level.updateNeighbourForOutputSignal(pos(x, y, z), BlockBridge.modernBlock(block));
    }

    public void scheduleBlockUpdate(int x, int y, int z, Block block, int delay) {
        mcc$level.scheduleTick(pos(x, y, z), BlockBridge.modernBlock(block), delay);
    }

    public void scheduleBlockUpdateWithPriority(int x, int y, int z, Block block, int delay, int priority) {
        scheduleBlockUpdate(x, y, z, block, delay);
    }

    public void func_147454_a(int x, int y, int z, Block block, int delay, int priority) {
        scheduleBlockUpdate(x, y, z, block, delay);
    }

    public boolean isBlockTickScheduledThisTick(int x, int y, int z, Block block) {
        return mcc$level.getBlockTicks().willTickThisTick(pos(x, y, z), BlockBridge.modernBlock(block));
    }

    public void addBlockEvent(int x, int y, int z, Block block, int id, int param) {
        Level level = mcc$fullLevel();
        if (level != null) level.blockEvent(pos(x, y, z), BlockBridge.modernBlock(block), id, param);
    }

    public boolean isBlockIndirectlyGettingPowered(int x, int y, int z) {
        return mcc$level instanceof SignalGetter s && s.hasNeighborSignal(pos(x, y, z));
    }

    public int getStrongestIndirectPower(int x, int y, int z) {
        return mcc$level instanceof SignalGetter s ? s.getBestNeighborSignal(pos(x, y, z)) : 0;
    }

    public boolean getIndirectPowerOutput(int x, int y, int z, int side) {
        return getIndirectPowerLevelTo(x, y, z, side) > 0;
    }

    public int getIndirectPowerLevelTo(int x, int y, int z, int side) {
        return mcc$level instanceof SignalGetter s ? s.getSignal(pos(x, y, z), Direction.from3DDataValue(side)) : 0;
    }

    public int getBlockPowerInput(int x, int y, int z) {
        return mcc$level instanceof SignalGetter s ? s.getDirectSignalTo(pos(x, y, z)) : 0;
    }

    @Override
    public int isBlockProvidingPowerTo(int x, int y, int z, int side) {
        BlockPos p = pos(x, y, z);
        return mcc$level.getBlockState(p).getDirectSignal(mcc$level, p, Direction.from3DDataValue(side));
    }

    // ------------------------------------------------------------------ tile entities

    @Override
    public TileEntity getTileEntity(int x, int y, int z) {
        return TileEntityBridge.legacyAt(this, pos(x, y, z));
    }

    public void setTileEntity(int x, int y, int z, TileEntity te) {
        TileEntityBridge.setAt(this, pos(x, y, z), te);
    }

    public void removeTileEntity(int x, int y, int z) {
        mcc$level.getChunk(pos(x, y, z)).removeBlockEntity(pos(x, y, z));
    }

    // ------------------------------------------------------------------ entities

    public boolean spawnEntityInWorld(Entity entity) {
        return EntityBridge.spawn(this, entity);
    }

    public void removeEntity(Entity entity) {
        entity.setDead();
    }

    public EntityPlayer getClosestPlayer(double x, double y, double z, double distance) {
        var p = mcc$level.getNearestPlayer(x, y, z, distance, false);
        return p == null ? null : (EntityPlayer) EntityBridge.wrap(p);
    }

    public EntityPlayer getClosestPlayerToEntity(Entity entity, double distance) {
        return getClosestPlayer(entity.mcc$x(), entity.mcc$y(), entity.mcc$z(), distance);
    }

    public EntityPlayer getClosestVulnerablePlayer(double x, double y, double z, double distance) {
        var p = mcc$level.getNearestPlayer(x, y, z, distance, true);
        return p == null ? null : (EntityPlayer) EntityBridge.wrap(p);
    }

    public EntityPlayer getClosestVulnerablePlayerToEntity(Entity entity, double distance) {
        return getClosestVulnerablePlayer(entity.mcc$x(), entity.mcc$y(), entity.mcc$z(), distance);
    }

    /** The legacy biome at a column (the modern biome at its surface, as its legacy counterpart). */
    public mccompat.v1_7_10.net.minecraft.world.biome.BiomeGenBase getBiomeGenForCoords(int x, int z) {
        int y = mcc$level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE_WG, x, z);
        var key = mcc$level.getBiome(pos(x, y, z)).unwrapKey().map(k -> k.location()).orElse(null);
        var legacy = org.kylepitcock.mccompat.v1_7_10.bridge.LegacyEntitySpawns.legacyBiome(key);
        return legacy != null ? legacy : mccompat.v1_7_10.net.minecraft.world.biome.BiomeGenBase.plains;
    }

    public mccompat.v1_7_10.net.minecraft.world.biome.BiomeGenBase getBiomeGenForCoordsBody(int x, int z) {
        return getBiomeGenForCoords(x, z);
    }

    public mccompat.v1_7_10.net.minecraft.world.chunk.Chunk getChunkFromBlockCoords(int x, int z) {
        return getChunkFromChunkCoords(x >> 4, z >> 4);
    }

    /** A view of the chunk; null when it is not available here (outside a generation region, or not loaded). */
    public mccompat.v1_7_10.net.minecraft.world.chunk.Chunk getChunkFromChunkCoords(int cx, int cz) {
        var access = mcc$level.getChunk(cx, cz, net.minecraft.world.level.chunk.status.ChunkStatus.EMPTY, false);
        return access == null ? null : new mccompat.v1_7_10.net.minecraft.world.chunk.Chunk(this, access);
    }

    public boolean chunkExists(int cx, int cz) {
        return mcc$level.hasChunk(cx, cz);
    }

    public EntityPlayer func_152378_a(java.util.UUID id) {
        var p = id == null ? null : mcc$level.getPlayerByUUID(id);
        return p == null ? null : (EntityPlayer) EntityBridge.wrap(p);
    }

    public EntityPlayer getPlayerEntityByName(String name) {
        for (var p : mcc$level.players()) {
            if (p.getGameProfile().getName().equals(name)) return (EntityPlayer) EntityBridge.wrap(p);
        }
        return null;
    }

    public EntityPlayer getPlayerEntityByUUID(java.util.UUID uuid) {
        var p = mcc$level.getPlayerByUUID(uuid);
        return p == null ? null : (EntityPlayer) EntityBridge.wrap(p);
    }

    public Entity getEntityByID(int id) {
        Level level = mcc$fullLevel();
        if (level == null) return null;
        var e = level.getEntity(id);
        return e == null ? null : EntityBridge.wrap(e);
    }

    public List getEntitiesWithinAABB(Class type, AxisAlignedBB box) {
        return selectEntitiesWithinAABB(type, box, null);
    }

    public List selectEntitiesWithinAABB(Class type, AxisAlignedBB box, mccompat.v1_7_10.net.minecraft.command.IEntitySelector selector) {
        List<Entity> out = new ArrayList<>();
        AABB modern = box.mcc$modern();
        for (var e : mcc$level.getEntities((net.minecraft.world.entity.Entity) null, modern, e -> true)) {
            Entity legacy = EntityBridge.wrap(e);
            if (type.isInstance(legacy) && (selector == null || selector.isEntityApplicable(legacy))) out.add(legacy);
        }
        return out;
    }

    public List getEntitiesWithinAABBExcludingEntity(Entity except, AxisAlignedBB box) {
        List<Entity> out = new ArrayList<>();
        var exceptModern = except == null ? null : except.mcc$entity();
        for (var e : mcc$level.getEntities(exceptModern, box.mcc$modern(), e -> true)) out.add(EntityBridge.wrap(e));
        return out;
    }

    public List getEntitiesWithinAABBExcludingEntity(Entity except, AxisAlignedBB box, mccompat.v1_7_10.net.minecraft.command.IEntitySelector selector) {
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
        return mcc$level.isUnobstructed(exceptModern, net.minecraft.world.phys.shapes.Shapes.create(box.mcc$modern()));
    }

    public boolean canPlaceEntityOnSide(Block block, int x, int y, int z, boolean skipCollision, int side, Entity placer, ItemStack stack) {
        Block existing = getBlock(x, y, z);
        AxisAlignedBB box = skipCollision ? null : block.getCollisionBoundingBoxFromPool(this, x, y, z);
        if (box != null && !checkNoEntityCollision(box, placer)) return false;
        return existing.isReplaceable(this, x, y, z) && block.canPlaceBlockOnSide(this, x, y, z, side);
    }

    // ------------------------------------------------------------------ sound, particles, explosions

    public void playSoundEffect(double x, double y, double z, String name, float volume, float pitch) {
        Level level = mcc$fullLevel();
        if (level != null) SoundBridge.play(level, null, x, y, z, name, volume, pitch);
    }

    public void playSoundAtEntity(Entity entity, String name, float volume, float pitch) {
        Level level = mcc$fullLevel();
        if (level != null) SoundBridge.play(level, null, entity.mcc$x(), entity.mcc$y(), entity.mcc$z(), name, volume, pitch);
    }

    public void playSoundToNearExcept(EntityPlayer player, String name, float volume, float pitch) {
        Level level = mcc$fullLevel();
        if (level != null) SoundBridge.play(level, player.mcc$player(), player.mcc$x(), player.mcc$y(), player.mcc$z(), name, volume, pitch);
    }

    public void playSound(double x, double y, double z, String name, float volume, float pitch, boolean distanceDelay) {
        playSoundEffect(x, y, z, name, volume, pitch);
    }

    public void playAuxSFX(int type, int x, int y, int z, int data) {
        mcc$level.levelEvent(type, pos(x, y, z), data);
    }

    public void playAuxSFXAtEntity(EntityPlayer player, int type, int x, int y, int z, int data) {
        mcc$level.levelEvent(player == null ? null : player.mcc$player(), type, pos(x, y, z), data);
    }

    public void spawnParticle(String name, double x, double y, double z, double vx, double vy, double vz) {
        SoundBridge.particle(mcc$level, name, x, y, z, vx, vy, vz);
    }

    public Explosion createExplosion(Entity entity, double x, double y, double z, float strength, boolean smoke) {
        return newExplosion(entity, x, y, z, strength, false, smoke);
    }

    public Explosion newExplosion(Entity entity, double x, double y, double z, float strength, boolean fire, boolean smoke) {
        Level level = mcc$fullLevel();
        if (level == null) return new Explosion(this, entity, x, y, z, strength);
        level.explode(entity == null ? null : entity.mcc$entity(), x, y, z, strength, fire,
                smoke ? Level.ExplosionInteraction.TNT : Level.ExplosionInteraction.NONE);
        return new Explosion(this, entity, x, y, z, strength);
    }

    // ------------------------------------------------------------------ time and weather

    public long getTotalWorldTime() {
        return mcc$level.getLevelData().getGameTime();
    }

    public long getWorldTime() {
        return mcc$level.getLevelData().getDayTime();
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
        Level level = mcc$fullLevel();
        return level != null && level.isRainingAt(pos(x, y, z));
    }

    public float getRainStrength(float partial) {
        Level level = mcc$fullLevel();
        return level == null ? 0 : level.getRainLevel(partial);
    }

    public int getMoonPhase() {
        return mcc$level.getMoonPhase();
    }

    public float getCelestialAngle(float partial) {
        return mcc$level.getTimeOfDay(partial);
    }

    public long getSeed() {
        if (mcc$level instanceof WorldGenLevel g) return g.getSeed();
        return mcc$fullLevel() instanceof ServerLevel s ? s.getSeed() : 0L;
    }

    private mccompat.v1_7_10.net.minecraft.world.storage.WorldInfo mcc$info;

    public mccompat.v1_7_10.net.minecraft.world.storage.WorldInfo getWorldInfo() {
        if (mcc$info == null) mcc$info = new mccompat.v1_7_10.net.minecraft.world.storage.WorldInfo(this);
        return mcc$info;
    }

    public ChunkCoordinates getSpawnPoint() {
        Level level = mcc$fullLevel();
        BlockPos spawn = level == null ? BlockPos.ZERO : level.getSharedSpawnPos();
        return new ChunkCoordinates(spawn.getX(), spawn.getY(), spawn.getZ());
    }

    public String getProviderName() {
        return provider.getDimensionName();
    }

    // ------------------------------------------------------------------ helpers used by other shims

    public int mcc$maxBuildHeight() {
        return mcc$level.getMaxBuildHeight();
    }

    public void mcc$playPlaceSound(Block block, int x, int y, int z) {
        playSoundEffect(x + 0.5, y + 0.5, z + 0.5, block.stepSound.getPlaceSound(), (block.stepSound.getVolume() + 1.0F) / 2.0F,
                block.stepSound.getPitch() * 0.8F);
    }

    public void mcc$dropXp(int x, int y, int z, int amount) {
        if (mcc$fullLevel() instanceof ServerLevel s && amount > 0) {
            net.minecraft.world.entity.ExperienceOrb.award(s, net.minecraft.world.phys.Vec3.atCenterOf(pos(x, y, z)), amount);
        }
    }

    @Override
    public String toString() {
        return "LegacyWorld{" + mcc$level + "}";
    }
}
