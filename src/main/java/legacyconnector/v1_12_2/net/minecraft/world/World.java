package legacyconnector.v1_12_2.net.minecraft.world;

import java.util.Random;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.tileentity.TileEntity;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.StatesBridge1122;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.TileEntityBridge1122;

/** 1.12.2 world: a view over a modern level. */
public class World {
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
        this.isRemote = level != null && level.isClientSide();
    }

    public net.minecraft.world.level.Level mcc$level() {
        return level;
    }

    private static net.minecraft.core.BlockPos modern(BlockPos p) {
        return new net.minecraft.core.BlockPos(p.getX(), p.getY(), p.getZ());
    }

    public IBlockState getBlockState(BlockPos pos) {
        IBlockState state = StatesBridge1122.toLegacy(level.getBlockState(modern(pos)));
        if (state == null) state = org.kylepitcock.legacyconnector.v1_12_2.bridge.Vanilla1122.airState();
        return state;
    }

    public boolean setBlockState(BlockPos pos, IBlockState state) {
        return setBlockState(pos, state, 3);
    }

    public boolean setBlockState(BlockPos pos, IBlockState state, int flags) {
        return level.setBlock(modern(pos), StatesBridge1122.toModern(state), flags);
    }

    public boolean isAirBlock(BlockPos pos) {
        return level.getBlockState(modern(pos)).isAir();
    }

    public boolean isBlockLoaded(BlockPos pos) {
        return level.hasChunkAt(modern(pos));
    }

    public TileEntity getTileEntity(BlockPos pos) {
        return TileEntityBridge1122.legacyAt(level, modern(pos));
    }

    public void markChunkDirty(BlockPos pos, TileEntity te) {
        level.getChunkAt(modern(pos)).setUnsaved(true);
    }

    public void notifyBlockUpdate(BlockPos pos, IBlockState oldState, IBlockState newState, int flags) {
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
        return level.removeBlock(modern(pos), false);
    }

    public boolean destroyBlock(BlockPos pos, boolean dropBlock) {
        return level.destroyBlock(modern(pos), dropBlock);
    }

    public void notifyNeighborsOfStateChange(BlockPos pos, legacyconnector.v1_12_2.net.minecraft.block.Block blockType, boolean updateObservers) {
        var block = level.getBlockState(modern(pos)).getBlock();
        level.updateNeighborsAt(modern(pos), block);
    }

    public int getLight(BlockPos pos) {
        return level.getMaxLocalRawBrightness(modern(pos));
    }

    public boolean canSeeSky(BlockPos pos) {
        return level.canSeeSky(modern(pos));
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
        int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, pos.getX(), pos.getZ());
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
        return level.getBlockState(p).isFaceSturdy(level, p, net.minecraft.core.Direction.byName(side.getName()));
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
        return level.getBlockState(p).isCollisionShapeFullBlock(level, p);
    }

    public boolean isValid(BlockPos pos) {
        return !level.isOutsideBuildHeight(modern(pos));
    }

    public boolean isOutsideBuildHeight(BlockPos pos) {
        return level.isOutsideBuildHeight(modern(pos));
    }

    public int getLightFromNeighbors(BlockPos pos) {
        return level.getMaxLocalRawBrightness(modern(pos));
    }

    public boolean canBlockSeeSky(BlockPos pos) {
        return level.canSeeSky(modern(pos));
    }

    public int getRedstonePower(BlockPos pos, legacyconnector.v1_12_2.net.minecraft.util.EnumFacing facing) {
        return level.getSignal(modern(pos), net.minecraft.core.Direction.byName(facing.getName()));
    }

    public int getStrongPower(BlockPos pos) {
        return level.getDirectSignalTo(modern(pos));
    }

    public BlockPos getTopSolidOrLiquidBlock(BlockPos pos) {
        int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, pos.getX(), pos.getZ());
        return new BlockPos(pos.getX(), y, pos.getZ());
    }

    public void scheduleUpdate(BlockPos pos, legacyconnector.v1_12_2.net.minecraft.block.Block block, int delay) {
        var p = modern(pos);
        level.scheduleTick(p, level.getBlockState(p).getBlock(), delay);
    }
}
