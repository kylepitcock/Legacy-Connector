package legacyconnector.v1_7_10.net.minecraft.server.management;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.FmlChannelBridge;

/** The chunk watchers of a world: mods reflect into it to send a packet to everyone who sees a chunk. */
public class PlayerManager {
    private final ServerLevel level;

    public PlayerManager(ServerLevel level) {
        this.level = level;
    }

    public PlayerInstance getOrCreateChunkWatcher(int chunkX, int chunkZ, boolean create) {
        return new PlayerInstance(chunkX, chunkZ);
    }

    public boolean func_152621_a(int chunkX, int chunkZ) {
        return !level.getChunkSource().chunkMap.getPlayers(new ChunkPos(chunkX, chunkZ), false).isEmpty();
    }

    /** The players watching one chunk. */
    public class PlayerInstance {
        private final int chunkX;
        private final int chunkZ;

        PlayerInstance(int chunkX, int chunkZ) {
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
        }

        public void sendToAllPlayersWatchingChunk(legacyconnector.v1_7_10.net.minecraft.network.Packet packet) {
            if (packet instanceof legacyconnector.v1_7_10.cpw.mods.fml.common.network.internal.FMLProxyPacket proxy) {
                FmlChannelBridge.sendToChunkWatchers(proxy, level, new ChunkPos(chunkX, chunkZ));
            }
        }
    }

    public boolean isPlayerWatchingChunk(legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayerMP player, int chunkX, int chunkZ) {
        return level.getChunkSource().chunkMap.getPlayers(new ChunkPos(chunkX, chunkZ), false).contains(player.mcc$serverPlayer());
    }

    public void removePlayer(legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayerMP player) {
    }
}
