package legacyconnector.v1_12_2.net.minecraft.server.management;

import java.util.ArrayList;
import java.util.List;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayerMP;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;

/**
 * Which players watch which chunks of a server world: a view over the modern level's chunk map (the modern game adds and
 * removes watchers itself, so the add/remove calls only matter for its own bookkeeping).
 */
public class PlayerChunkMap {
    private final net.minecraft.server.level.ServerLevel level;

    public PlayerChunkMap(net.minecraft.server.level.ServerLevel level) {
        this.level = level;
    }

    public PlayerChunkMap(legacyconnector.v1_12_2.net.minecraft.world.WorldServer world) {
        this(world.mcc$serverLevel());
    }

    public net.minecraft.server.level.ServerLevel mcc$level() {
        return level;
    }

    public legacyconnector.v1_12_2.net.minecraft.world.WorldServer getWorldServer() {
        return (legacyconnector.v1_12_2.net.minecraft.world.WorldServer) org.kylepitcock.legacyconnector.v1_12_2.bridge.WorldBridge1122.wrap(level);
    }

    List<net.minecraft.server.level.ServerPlayer> mcc$watchers(int x, int z) {
        return level == null ? List.of() : level.getChunkSource().chunkMap.getPlayers(new net.minecraft.world.level.ChunkPos(x, z), false);
    }

    public boolean contains(int chunkX, int chunkZ) {
        return level != null && level.getChunkSource().chunkMap.getVisibleChunkIfPresent(net.minecraft.world.level.ChunkPos.asLong(chunkX, chunkZ)) != null;
    }

    /** The chunk's watcher entry, or null when the chunk is not loaded for players. */
    public PlayerChunkMapEntry getEntry(int x, int z) {
        return contains(x, z) ? new PlayerChunkMapEntry(this, x, z) : null;
    }

    public PlayerChunkMapEntry getOrCreateEntry(int chunkX, int chunkZ) {
        return new PlayerChunkMapEntry(this, chunkX, chunkZ);
    }

    public boolean isPlayerWatchingChunk(EntityPlayerMP player, int chunkX, int chunkZ) {
        var sp = player == null ? null : player.mcc$serverPlayer();
        return sp != null && mcc$watchers(chunkX, chunkZ).contains(sp);
    }

    /** Sends the block's new state to the players watching its chunk. */
    public void markBlockForUpdate(BlockPos pos) {
        if (level != null && pos != null) level.getChunkSource().blockChanged(pos.mcc$modern());
    }

    public void addPlayer(EntityPlayerMP player) {
    }

    public void removePlayer(EntityPlayerMP player) {
    }

    public void updateMovingPlayer(EntityPlayerMP player) {
    }

    public void tick() {
    }

    public List<EntityPlayerMP> mcc$players(int x, int z) {
        List<EntityPlayerMP> out = new ArrayList<>();
        for (var sp : mcc$watchers(x, z)) {
            if (org.kylepitcock.legacyconnector.v1_12_2.bridge.EntityBridge1122.wrap(sp) instanceof EntityPlayerMP mp) out.add(mp);
        }
        return out;
    }

    public static int getFurthestViewableBlock(int distance) {
        return distance * 16 - 16;
    }
}
