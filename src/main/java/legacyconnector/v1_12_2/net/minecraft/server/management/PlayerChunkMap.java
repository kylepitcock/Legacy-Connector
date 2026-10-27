package legacyconnector.v1_12_2.net.minecraft.server.management;

import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayerMP;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.WorldServer;

/**
 * 1.12.2 tracker of which players see which chunks, over the server level's modern chunk map: who watches a chunk, and resending
 * a changed block to them. Player view tracking itself is the modern game's.
 */
public class PlayerChunkMap {
    private final WorldServer world;

    public PlayerChunkMap(WorldServer world) {
        this.world = world;
    }

    private net.minecraft.server.level.ServerLevel level() {
        return world == null ? null : world.mcc$serverLevel();
    }

    public WorldServer getWorldServer() {
        return world;
    }

    public void tick() {
    }

    /** Whether any player watches the chunk. */
    public boolean contains(int chunkX, int chunkZ) {
        var level = level();
        return level != null && !level.getChunkSource().chunkMap.getPlayers(new net.minecraft.world.level.ChunkPos(chunkX, chunkZ), false).isEmpty();
    }

    /** Sends the block's current state to the players watching it. */
    public void markBlockForUpdate(BlockPos pos) {
        var level = level();
        if (level != null) level.getChunkSource().blockChanged(new net.minecraft.core.BlockPos(pos.getX(), pos.getY(), pos.getZ()));
    }

    public void addPlayer(EntityPlayerMP player) {
    }

    public void removePlayer(EntityPlayerMP player) {
    }

    public void updateMovingPlayer(EntityPlayerMP player) {
    }

    public boolean isPlayerWatchingChunk(EntityPlayerMP player, int chunkX, int chunkZ) {
        var level = level();
        if (level == null || !(player != null && player.mcc$entity instanceof net.minecraft.server.level.ServerPlayer p)) return false;
        return level.getChunkSource().chunkMap.getPlayers(new net.minecraft.world.level.ChunkPos(chunkX, chunkZ), false).contains(p);
    }

    public void setPlayerViewRadius(int radius) {
    }

    public static int getFurthestViewableBlock(int distance) {
        return distance * 16 - 16;
    }
}
