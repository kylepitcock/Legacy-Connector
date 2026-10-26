package legacyconnector.v1_7_10.net.minecraft.server.management;

import java.util.ArrayList;
import java.util.List;
import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.EntityBridge;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.FmlChannelBridge;

/**
 * The chunk watchers of a world, over the modern chunk map: mods send a packet to everyone who sees a chunk, mark
 * blocks for resending and read who watches a chunk.
 */
public class PlayerManager {
    private final ServerLevel level;

    public PlayerManager(ServerLevel level) {
        this.level = level;
    }

    public legacyconnector.v1_7_10.net.minecraft.world.WorldServer getWorldServer() {
        return (legacyconnector.v1_7_10.net.minecraft.world.WorldServer) legacyconnector.v1_7_10.net.minecraft.world.WorldServer.mcc$wrap(level);
    }

    /** The watchers of a chunk; null when nobody has it loaded and {@code create} is false. */
    public PlayerInstance getOrCreateChunkWatcher(int chunkX, int chunkZ, boolean create) {
        if (!create && level.getChunkSource().chunkMap.getVisibleChunkIfPresent(ChunkPos.asLong(chunkX, chunkZ)) == null) return null;
        return new PlayerInstance(chunkX, chunkZ);
    }

    public boolean func_152621_a(int chunkX, int chunkZ) {
        return !level.getChunkSource().chunkMap.getPlayers(new ChunkPos(chunkX, chunkZ), false).isEmpty();
    }

    /** Resends the block to every player who sees it (on the next chunk update). */
    public void markBlockForUpdate(int x, int y, int z) {
        level.getChunkSource().blockChanged(new BlockPos(x, y, z));
    }

    /** How far players see, in blocks, for a view distance in chunks. */
    public static int getFurthestViewableBlock(int distance) {
        return distance * 16 - 16;
    }

    public void updatePlayerInstances() {
    }

    public void addPlayer(EntityPlayerMP player) {
    }

    public void updatePlayerPertinentChunks(EntityPlayerMP player) {
    }

    public void filterChunkLoadQueue(EntityPlayerMP player) {
    }

    public void func_152622_a(int viewDistance) {
    }

    /** The players watching one chunk. */
    public class PlayerInstance {
        private final int chunkX;
        private final int chunkZ;

        /** Phantom: redirected to {@code mcc$get$playersWatchingChunk}. */
        @Deprecated
        public List playersWatchingChunk;

        PlayerInstance(int chunkX, int chunkZ) {
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
        }

        public static List mcc$get$playersWatchingChunk(PlayerInstance instance) {
            List<EntityPlayerMP> out = new ArrayList<>();
            for (var p : instance.mcc$level().getChunkSource().chunkMap.getPlayers(new ChunkPos(instance.chunkX, instance.chunkZ), false)) {
                out.add((EntityPlayerMP) EntityBridge.wrap(p));
            }
            return out;
        }

        ServerLevel mcc$level() {
            return level;
        }

        private ChunkHolder mcc$holder() {
            return level.getChunkSource().chunkMap.getVisibleChunkIfPresent(ChunkPos.asLong(chunkX, chunkZ));
        }

        public void sendToAllPlayersWatchingChunk(legacyconnector.v1_7_10.net.minecraft.network.Packet packet) {
            if (packet instanceof legacyconnector.v1_7_10.cpw.mods.fml.common.network.internal.FMLProxyPacket proxy) {
                FmlChannelBridge.sendToChunkWatchers(proxy, level, new ChunkPos(chunkX, chunkZ));
            } else if (packet != null) {
                for (var p : level.getChunkSource().chunkMap.getPlayers(new ChunkPos(chunkX, chunkZ), false)) {
                    new legacyconnector.v1_7_10.net.minecraft.network.NetHandlerPlayServer((EntityPlayerMP) EntityBridge.wrap(p)).sendPacket(packet);
                }
            }
        }

        /** Marks a block (chunk-local coordinates) as changed. */
        public void flagChunkForUpdate(int x, int y, int z) {
            ChunkHolder holder = mcc$holder();
            if (holder != null) holder.blockChanged(new BlockPos((chunkX << 4) + x, y, (chunkZ << 4) + z));
        }

        /** Sends the blocks changed in this chunk to its watchers now. */
        public void sendChunkUpdate() {
            ChunkHolder holder = mcc$holder();
            if (holder == null) return;
            var chunk = holder.getTickingChunk();
            if (chunk != null) holder.broadcastChanges(chunk);
        }

        public void addPlayer(EntityPlayerMP player) {
        }

        public void removePlayer(EntityPlayerMP player) {
        }

        public void processChunk() {
        }
    }

    public boolean isPlayerWatchingChunk(EntityPlayerMP player, int chunkX, int chunkZ) {
        return level.getChunkSource().chunkMap.getPlayers(new ChunkPos(chunkX, chunkZ), false).contains(player.mcc$serverPlayer());
    }

    public void removePlayer(EntityPlayerMP player) {
    }
}
