package legacyconnector.v1_12_2.net.minecraftforge.event.world;

import legacyconnector.v1_12_2.net.minecraft.world.chunk.Chunk;

/** Forge's chunk events, posted when the modern game loads and unloads a chunk. */
public class ChunkEvent extends WorldEvent {
    private final Chunk chunk;

    public ChunkEvent(Chunk chunk) {
        super(chunk.getWorld());
        this.chunk = chunk;
    }

    public Chunk getChunk() {
        return chunk;
    }

    public static class Load extends ChunkEvent {
        public Load(Chunk chunk) {
            super(chunk);
        }
    }

    public static class Unload extends ChunkEvent {
        public Unload(Chunk chunk) {
            super(chunk);
        }
    }
}
