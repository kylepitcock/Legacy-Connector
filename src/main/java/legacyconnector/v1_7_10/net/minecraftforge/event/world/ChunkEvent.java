package legacyconnector.v1_7_10.net.minecraftforge.event.world;

import legacyconnector.v1_7_10.net.minecraft.world.chunk.Chunk;

/** A chunk loaded or unloaded. */
public class ChunkEvent extends WorldEvent {
    private final Chunk chunk;

    public ChunkEvent(Chunk chunk) {
        super(chunk.worldObj);
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
