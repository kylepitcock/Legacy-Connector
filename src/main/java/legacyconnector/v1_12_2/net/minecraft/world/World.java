package legacyconnector.v1_12_2.net.minecraft.world;

import java.util.Random;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.tileentity.TileEntity;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.StatesBridge1122;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.TileEntityBridge1122;

/**
 * 1.12.2 world: a view over a modern level, or over a world generation region while legacy generators run (block reads and
 * writes go to the region; everything else to its level).
 */
public class World implements IBlockAccess {
    private final net.minecraft.world.level.Level level;

    public final boolean isRemote;
    public final Random rand = new Random();
    /** Phantom field: accesses are redirected to {@code mcc$get$provider}. */
    @Deprecated
    public WorldProvider provider;
    private WorldProvider mcc$provider;
    /** Phantom field: accesses are redirected to {@code mcc$get$playerEntities} (a snapshot of the level's players). */
    @Deprecated
    public java.util.List<legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer> playerEntities;

    protected World(net.minecraft.world.level.Level level) {
        this.level = level;
        this.mcc$region = null;
        this.isRemote = level != null && level.isClientSide();
    }

    /** A view of a world generation region (block access goes to the region, the rest to its server level). */
    protected World(net.minecraft.world.level.WorldGenLevel region) {
        this.level = region.getLevel();
        this.mcc$region = region;
        this.isRemote = false;
    }

    private final net.minecraft.world.level.WorldGenLevel mcc$region;

    /**
     * While set on a thread, every view of the region's level reads and writes through the region (a mod generator that
     * reaches the world another way, {@code DimensionManager.getWorld}, must not touch the live level from a generation thread).
     */
    private static final ThreadLocal<net.minecraft.world.level.WorldGenLevel> REGION = new ThreadLocal<>();

    public static net.minecraft.world.level.WorldGenLevel mcc$enterRegion(net.minecraft.world.level.WorldGenLevel region) {
        var previous = REGION.get();
        REGION.set(region);
        return previous;
    }

    public static void mcc$leaveRegion(net.minecraft.world.level.WorldGenLevel previous) {
        if (previous == null) REGION.remove();
        else REGION.set(previous);
    }

    /** Where blocks are read and written: the generation region of this view or of this thread, else the level. */
    public net.minecraft.world.level.LevelAccessor mcc$access() {
        if (mcc$region != null) return mcc$region;
        var region = REGION.get();
        if (region != null && level != null && region.getLevel() == level) return region;
        return level;
    }

    /** The generation region blocks go to, or null during gameplay (then block updates, sounds and entities may reach the level). */
    public net.minecraft.world.level.WorldGenLevel mcc$region() {
        return mcc$access() instanceof net.minecraft.world.level.WorldGenLevel g && !(g instanceof net.minecraft.world.level.Level) ? g : null;
    }

    /** Views of one level are equal (1.12 had one World per dimension; generation hands out a region view per chunk). */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        return o instanceof World other && level != null && other.level == level;
    }

    @Override
    public int hashCode() {
        return level != null ? System.identityHashCode(level) : System.identityHashCode(this);
    }

    public net.minecraft.world.level.Level mcc$level() {
        return level;
    }

    private static net.minecraft.core.BlockPos modern(BlockPos p) {
        return new net.minecraft.core.BlockPos(p.getX(), p.getY(), p.getZ());
    }

    public IBlockState getBlockState(BlockPos pos) {
        IBlockState state = StatesBridge1122.toLegacy(mcc$access().getBlockState(modern(pos)));
        if (state == null) state = org.kylepitcock.legacyconnector.v1_12_2.bridge.Vanilla1122.airState();
        return state;
    }

    public boolean setBlockState(BlockPos pos, IBlockState state) {
        return setBlockState(pos, state, 3);
    }

    public boolean setBlockState(BlockPos pos, IBlockState state, int flags) {
        var region = mcc$region();
        if (region != null) {
            var p = modern(pos);
            if (!region.ensureCanWrite(p)) return false;
            var modernState = StatesBridge1122.toModern(state);
            boolean placed = region.setBlock(p, modernState, flags & ~1 | 16);
            if (placed) mcc$ensureGeneratedBlockEntity(region, p, modernState);
            return placed;
        }
        return level.setBlock(modern(pos), StatesBridge1122.toModern(state), flags);
    }

    public boolean isAirBlock(BlockPos pos) {
        return mcc$access().getBlockState(modern(pos)).isAir();
    }

    public boolean isBlockLoaded(BlockPos pos) {
        var region = mcc$region();
        if (region != null) return region.hasChunk(pos.getX() >> 4, pos.getZ() >> 4);
        return level.hasChunkAt(modern(pos));
    }

    public TileEntity getTileEntity(BlockPos pos) {
        var be = mcc$access().getBlockEntity(modern(pos));
        return be instanceof org.kylepitcock.legacyconnector.v1_12_2.adapter.Legacy1122BlockEntity l ? l.legacy() : null;
    }

    public void markChunkDirty(BlockPos pos, TileEntity te) {
        mcc$access().getChunk(modern(pos)).setUnsaved(true);
    }

    public void notifyBlockUpdate(BlockPos pos, IBlockState oldState, IBlockState newState, int flags) {
        if (mcc$region() != null) return;
        level.sendBlockUpdated(modern(pos), StatesBridge1122.toModern(oldState), StatesBridge1122.toModern(newState), flags);
    }

    public long getTotalWorldTime() {
        return level.getGameTime();
    }

    public long getWorldTime() {
        return level.getDayTime();
    }

    public boolean isDaytime() {
        return level.isDay();
    }

    public boolean setBlockToAir(BlockPos pos) {
        return mcc$region() != null ? setBlockState(pos, org.kylepitcock.legacyconnector.v1_12_2.bridge.Vanilla1122.airState(), 3)
                : level.removeBlock(modern(pos), false);
    }

    public boolean destroyBlock(BlockPos pos, boolean dropBlock) {
        return mcc$access().destroyBlock(modern(pos), dropBlock && mcc$region() == null);
    }

    public void notifyNeighborsOfStateChange(BlockPos pos, legacyconnector.v1_12_2.net.minecraft.block.Block blockType, boolean updateObservers) {
        if (mcc$region() != null) return;
        var block = level.getBlockState(modern(pos)).getBlock();
        level.updateNeighborsAt(modern(pos), block);
    }

    public int getLight(BlockPos pos) {
        return mcc$access().getMaxLocalRawBrightness(modern(pos));
    }

    public boolean canSeeSky(BlockPos pos) {
        return mcc$access().canSeeSky(modern(pos));
    }

    public boolean isRaining() {
        return level.isRaining();
    }

    public boolean isThundering() {
        return level.isThundering();
    }

    public boolean isRainingAt(BlockPos pos) {
        return level.isRainingAt(modern(pos));
    }

    public int getSeaLevel() {
        return level.getSeaLevel();
    }

    public int getHeight() {
        return level.getHeight();
    }

    public int getActualHeight() {
        return level.getHeight();
    }

    public BlockPos getHeight(BlockPos pos) {
        int y = mcc$access().getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, pos.getX(), pos.getZ());
        return new BlockPos(pos.getX(), y, pos.getZ());
    }

    public boolean isBlockPowered(BlockPos pos) {
        return level.hasNeighborSignal(modern(pos));
    }

    public int isBlockIndirectlyGettingPowered(BlockPos pos) {
        return level.getBestNeighborSignal(modern(pos));
    }

    public boolean isSideSolid(BlockPos pos, legacyconnector.v1_12_2.net.minecraft.util.EnumFacing side) {
        var p = modern(pos);
        var access = mcc$access();
        return access.getBlockState(p).isFaceSturdy(access, p, net.minecraft.core.Direction.byName(side.getName()));
    }

    public void playSound(legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer player, BlockPos pos,
            legacyconnector.v1_12_2.net.minecraft.util.SoundEvent sound, legacyconnector.v1_12_2.net.minecraft.util.SoundCategory category, float volume, float pitch) {
        if (sound == null || sound.mcc$modern == null) return;
        net.minecraft.world.entity.player.Player except = player == null ? null : ((legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer) player).mcc$player();
        net.minecraft.sounds.SoundSource source = net.minecraft.sounds.SoundSource.valueOf(category.getName().toUpperCase(java.util.Locale.ROOT).replace("PLAYERS", "PLAYERS"));
        level.playSound(except, modern(pos), sound.mcc$modern, source, volume, pitch);
    }

    public boolean isDaytimeNow() {
        return isDaytime();
    }

    // ------------------------------------------------------------------ provider, server, rules

    public static WorldProvider mcc$get$provider(World w) {
        if (w.mcc$provider == null) w.mcc$provider = new WorldProvider(w);
        return w.mcc$provider;
    }

    public static java.util.List<legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer> mcc$get$playerEntities(World w) {
        java.util.List<legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer> out = new java.util.ArrayList<>();
        for (var p : w.level.players()) {
            if (mcc$view(p) instanceof legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer player) out.add(player);
        }
        return out;
    }

    private static legacyconnector.v1_12_2.net.minecraft.entity.Entity mcc$view(net.minecraft.world.entity.Entity e) {
        return org.kylepitcock.legacyconnector.v1_12_2.bridge.EntityBridge1122.wrap(e);
    }

    public legacyconnector.v1_12_2.net.minecraft.server.MinecraftServer getMinecraftServer() {
        return level.getServer() == null ? null : legacyconnector.v1_12_2.net.minecraft.server.MinecraftServer.mcc$instance();
    }

    public legacyconnector.v1_12_2.net.minecraft.world.storage.WorldInfo getWorldInfo() {
        return new legacyconnector.v1_12_2.net.minecraft.world.storage.WorldInfo(level);
    }

    public GameRules getGameRules() {
        return new GameRules(level.getGameRules());
    }

    public long getSeed() {
        return level instanceof net.minecraft.server.level.ServerLevel s ? s.getSeed() : 0L;
    }

    public BlockPos getSpawnPoint() {
        var p = level.getSharedSpawnPos();
        return new BlockPos(p.getX(), p.getY(), p.getZ());
    }

    public int getDifficultyId() {
        return level.getDifficulty().getId();
    }

    // ------------------------------------------------------------------ saved data

    private static final java.util.Map<Object, legacyconnector.v1_12_2.net.minecraft.world.storage.MapStorage> STORAGES =
            java.util.Collections.synchronizedMap(new java.util.WeakHashMap<>());
    private static final legacyconnector.v1_12_2.net.minecraft.world.storage.MapStorage CLIENT_STORAGE =
            new legacyconnector.v1_12_2.net.minecraft.world.storage.MapStorage(null);

    private static legacyconnector.v1_12_2.net.minecraft.world.storage.MapStorage mcc$storage(net.minecraft.server.level.ServerLevel level) {
        if (level == null) return CLIENT_STORAGE;
        var data = level.getDataStorage();
        return STORAGES.computeIfAbsent(data, d -> new legacyconnector.v1_12_2.net.minecraft.world.storage.MapStorage(data));
    }

    /** Saved data shared by all dimensions (kept with the overworld's, as 1.12.2 kept it in the world's data folder). */
    public legacyconnector.v1_12_2.net.minecraft.world.storage.MapStorage getMapStorage() {
        return mcc$storage(level instanceof net.minecraft.server.level.ServerLevel s ? s.getServer().overworld() : null);
    }

    /** Saved data of this dimension. */
    public legacyconnector.v1_12_2.net.minecraft.world.storage.MapStorage getPerWorldStorage() {
        return mcc$storage(level instanceof net.minecraft.server.level.ServerLevel s ? s : null);
    }

    public legacyconnector.v1_12_2.net.minecraft.world.storage.WorldSavedData loadData(Class<? extends legacyconnector.v1_12_2.net.minecraft.world.storage.WorldSavedData> type, String name) {
        return getMapStorage().getOrLoadData(type, name);
    }

    public void setData(String name, legacyconnector.v1_12_2.net.minecraft.world.storage.WorldSavedData data) {
        getMapStorage().setData(name, data);
    }

    public int getUniqueDataId(String key) {
        return getMapStorage().getUniqueDataId(key);
    }

    // ------------------------------------------------------------------ entities

    public legacyconnector.v1_12_2.net.minecraft.entity.Entity getEntityByID(int id) {
        return mcc$view(level.getEntity(id));
    }

    /** Adds an entity bound to a modern one that is not in a level yet; legacy-only entities cannot be spawned yet. */
    public boolean spawnEntity(legacyconnector.v1_12_2.net.minecraft.entity.Entity entity) {
        if (entity == null || entity.mcc$entity == null || entity.mcc$entity.isAddedToLevel()) return false;
        return level.addFreshEntity(entity.mcc$entity);
    }

    public void removeEntity(legacyconnector.v1_12_2.net.minecraft.entity.Entity entity) {
        if (entity != null && entity.mcc$entity != null) entity.mcc$entity.discard();
    }

    public <T extends legacyconnector.v1_12_2.net.minecraft.entity.Entity> java.util.List<T> getEntitiesWithinAABB(Class<? extends T> type,
            legacyconnector.v1_12_2.net.minecraft.util.math.AxisAlignedBB box) {
        return getEntitiesWithinAABB(type, box, null);
    }

    /** Legacy views of the modern entities in the box that are instances of the legacy type (a view's class follows its modern entity). */
    @SuppressWarnings("unchecked")
    public <T extends legacyconnector.v1_12_2.net.minecraft.entity.Entity> java.util.List<T> getEntitiesWithinAABB(Class<? extends T> type,
            legacyconnector.v1_12_2.net.minecraft.util.math.AxisAlignedBB box, com.google.common.base.Predicate<? super T> filter) {
        java.util.List<T> out = new java.util.ArrayList<>();
        if (box == null) return out;
        for (var e : level.getEntities((net.minecraft.world.entity.Entity) null, box.mcc$modern(), x -> true)) {
            var view = mcc$view(e);
            if (view != null && type.isInstance(view) && (filter == null || filter.apply((T) view))) out.add((T) view);
        }
        return out;
    }

    public java.util.List<legacyconnector.v1_12_2.net.minecraft.entity.Entity> getEntitiesWithinAABBExcludingEntity(
            legacyconnector.v1_12_2.net.minecraft.entity.Entity except, legacyconnector.v1_12_2.net.minecraft.util.math.AxisAlignedBB box) {
        return getEntitiesInAABBexcluding(except, box, null);
    }

    public java.util.List<legacyconnector.v1_12_2.net.minecraft.entity.Entity> getEntitiesInAABBexcluding(
            legacyconnector.v1_12_2.net.minecraft.entity.Entity except, legacyconnector.v1_12_2.net.minecraft.util.math.AxisAlignedBB box,
            com.google.common.base.Predicate<? super legacyconnector.v1_12_2.net.minecraft.entity.Entity> filter) {
        java.util.List<legacyconnector.v1_12_2.net.minecraft.entity.Entity> out = new java.util.ArrayList<>();
        if (box == null) return out;
        for (var e : level.getEntities(except == null ? null : except.mcc$entity, box.mcc$modern(), x -> true)) {
            var view = mcc$view(e);
            if (view != null && (filter == null || filter.apply(view))) out.add(view);
        }
        return out;
    }

    public legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer getPlayerEntityByUUID(java.util.UUID uuid) {
        var p = uuid == null ? null : level.getPlayerByUUID(uuid);
        return mcc$view(p) instanceof legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer player ? player : null;
    }

    public legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer getPlayerEntityByName(String name) {
        for (var p : level.players()) {
            if (p.getGameProfile().getName().equals(name) && mcc$view(p) instanceof legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer player) return player;
        }
        return null;
    }

    public legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer getClosestPlayer(double x, double y, double z, double distance, boolean spectators) {
        net.minecraft.world.entity.player.Player best = null;
        double bestSq = Double.MAX_VALUE;
        for (var p : level.players()) {
            if (!spectators && p.isSpectator()) continue;
            double d = p.distanceToSqr(x, y, z);
            if ((distance < 0 || d < distance * distance) && d < bestSq) {
                best = p;
                bestSq = d;
            }
        }
        return mcc$view(best) instanceof legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer player ? player : null;
    }

    public legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer getClosestPlayerToEntity(legacyconnector.v1_12_2.net.minecraft.entity.Entity entity, double distance) {
        return getClosestPlayer(legacyconnector.v1_12_2.net.minecraft.entity.Entity.mcc$get$posX(entity), legacyconnector.v1_12_2.net.minecraft.entity.Entity.mcc$get$posY(entity),
                legacyconnector.v1_12_2.net.minecraft.entity.Entity.mcc$get$posZ(entity), distance, false);
    }

    public boolean isAnyPlayerWithinRangeAt(double x, double y, double z, double range) {
        return level.hasNearbyAlivePlayer(x, y, z, range);
    }

    // ------------------------------------------------------------------ blocks and sounds

    public void removeTileEntity(BlockPos pos) {
        level.removeBlockEntity(modern(pos));
    }

    public void playSound(legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer player, double x, double y, double z,
            legacyconnector.v1_12_2.net.minecraft.util.SoundEvent sound, legacyconnector.v1_12_2.net.minecraft.util.SoundCategory category, float volume, float pitch) {
        if (sound == null || sound.mcc$modern == null) return;
        net.minecraft.world.entity.player.Player except = player == null ? null : player.mcc$player();
        level.playSound(except, x, y, z, sound.mcc$modern, mcc$source(category), volume, pitch);
    }

    private static net.minecraft.sounds.SoundSource mcc$source(legacyconnector.v1_12_2.net.minecraft.util.SoundCategory category) {
        if (category == null) return net.minecraft.sounds.SoundSource.MASTER;
        try {
            return net.minecraft.sounds.SoundSource.valueOf(category.getName().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return net.minecraft.sounds.SoundSource.MASTER;
        }
    }

    public boolean isBlockFullCube(BlockPos pos) {
        var p = modern(pos);
        var access = mcc$access();
        return access.getBlockState(p).isCollisionShapeFullBlock(access, p);
    }

    public boolean isValid(BlockPos pos) {
        return !level.isOutsideBuildHeight(modern(pos));
    }

    public boolean isOutsideBuildHeight(BlockPos pos) {
        return level.isOutsideBuildHeight(modern(pos));
    }

    public int getLightFromNeighbors(BlockPos pos) {
        return mcc$access().getMaxLocalRawBrightness(modern(pos));
    }

    public boolean canBlockSeeSky(BlockPos pos) {
        return mcc$access().canSeeSky(modern(pos));
    }

    public int getRedstonePower(BlockPos pos, legacyconnector.v1_12_2.net.minecraft.util.EnumFacing facing) {
        return level.getSignal(modern(pos), net.minecraft.core.Direction.byName(facing.getName()));
    }

    public int getStrongPower(BlockPos pos) {
        return level.getDirectSignalTo(modern(pos));
    }

    public BlockPos getTopSolidOrLiquidBlock(BlockPos pos) {
        int y = mcc$access().getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, pos.getX(), pos.getZ());
        return new BlockPos(pos.getX(), y, pos.getZ());
    }

    public void scheduleUpdate(BlockPos pos, legacyconnector.v1_12_2.net.minecraft.block.Block block, int delay) {
        var p = modern(pos);
        var access = mcc$access();
        access.scheduleTick(p, access.getBlockState(p).getBlock(), delay);
    }
}
