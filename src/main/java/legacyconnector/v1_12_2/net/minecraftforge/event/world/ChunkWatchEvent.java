package legacyconnector.v1_12_2.net.minecraftforge.event.world;

import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayerMP;
import legacyconnector.v1_12_2.net.minecraft.util.math.ChunkPos;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Event;

/** Forge 1.12.2: a player starts (Watch) or stops (UnWatch) receiving a chunk; fed from the modern events. */
public class ChunkWatchEvent extends Event {
    private final ChunkPos chunk;
    private final EntityPlayerMP player;
    private final legacyconnector.v1_12_2.net.minecraft.world.chunk.Chunk chunkInstance;

    public ChunkWatchEvent(ChunkPos chunk, EntityPlayerMP player) {
        this(chunk, player, null);
    }

    public ChunkWatchEvent(ChunkPos chunk, EntityPlayerMP player, legacyconnector.v1_12_2.net.minecraft.world.chunk.Chunk chunkInstance) {
        this.chunk = chunk;
        this.player = player;
        this.chunkInstance = chunkInstance;
    }

    public ChunkPos getChunk() {
        return chunk;
    }

    public EntityPlayerMP getPlayer() {
        return player;
    }

    public legacyconnector.v1_12_2.net.minecraft.world.chunk.Chunk getChunkInstance() {
        return chunkInstance;
    }

    public static class Watch extends ChunkWatchEvent {
        public Watch(ChunkPos chunk, EntityPlayerMP player) {
            super(chunk, player);
        }

        public Watch(legacyconnector.v1_12_2.net.minecraft.world.chunk.Chunk chunk, EntityPlayerMP player) {
            super(chunk.getPos(), player, chunk);
        }
    }

    public static class UnWatch extends ChunkWatchEvent {
        public UnWatch(ChunkPos chunk, EntityPlayerMP player) {
            super(chunk, player);
        }
    }
}
