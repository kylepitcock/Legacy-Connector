package legacyconnector.v1_12_2.net.minecraft.server.management;

import java.util.List;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayerMP;
import legacyconnector.v1_12_2.net.minecraft.util.math.ChunkPos;

/** One chunk's watchers: a view over the modern chunk map (its players are read live). */
public class PlayerChunkMapEntry {
    private final PlayerChunkMap playerChunkMap;
    private final ChunkPos pos;

    /** Phantom field: reads are redirected to {@code mcc$get$players} (the players watching the chunk now). */
    @Deprecated
    public List<EntityPlayerMP> players;

    public PlayerChunkMapEntry(PlayerChunkMap mapIn, int chunkX, int chunkZ) {
        this.playerChunkMap = mapIn;
        this.pos = new ChunkPos(chunkX, chunkZ);
    }

    public static List<EntityPlayerMP> mcc$get$players(PlayerChunkMapEntry e) {
        return e.mcc$base$getWatchingPlayers();
    }

    public ChunkPos getPos() {
        return pos;
    }

    public List<EntityPlayerMP> getWatchingPlayers() {
        return mcc$base$getWatchingPlayers();
    }

    /** The value of {@link #getWatchingPlayers()}, for the phantom field accessor (mods override the getter to read the field). */
    private List<EntityPlayerMP> mcc$base$getWatchingPlayers() {
        return playerChunkMap.mcc$players(pos.x, pos.z);
    }


    public boolean containsPlayer(EntityPlayerMP player) {
        return playerChunkMap.isPlayerWatchingChunk(player, pos.x, pos.z);
    }

    public boolean hasPlayerMatching(com.google.common.base.Predicate<EntityPlayerMP> predicate) {
        for (EntityPlayerMP p : getWatchingPlayers()) if (predicate.apply(p)) return true;
        return false;
    }

    /** The chunk was sent to its watchers (here: it is loaded and someone watches it). */
    public boolean isSentToPlayers() {
        return playerChunkMap.contains(pos.x, pos.z) && !getWatchingPlayers().isEmpty();
    }

    public void sendPacket(legacyconnector.v1_12_2.net.minecraft.network.Packet<?> packetIn) {
        var modern = packetIn == null ? null : packetIn.mcc$modern();
        if (modern == null) return;
        for (var sp : playerChunkMap.mcc$watchers(pos.x, pos.z)) sp.connection.send(modern);
    }

    public void blockChanged(int x, int y, int z) {
        playerChunkMap.markBlockForUpdate(new legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos((pos.x << 4) + x, y, (pos.z << 4) + z));
    }

    /** The modern chunk map tracks watchers itself. */
    public void addPlayer(EntityPlayerMP player) {
    }

    public void removePlayer(EntityPlayerMP player) {
    }

    public void update() {
    }
}
