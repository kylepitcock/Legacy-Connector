package legacyconnector.v1_7_10.net.minecraft.world.chunk;

/**
 * 1.7.10's chunk provider. Mods implement it for world generation (only the generation methods matter then) and call
 * it on {@code world.getChunkProvider()} (see {@link legacyconnector.v1_7_10.net.minecraft.world.World#getChunkProvider}).
 */
public interface IChunkProvider {
    boolean chunkExists(int x, int z);

    String makeString();

    int getLoadedChunkCount();

    default Chunk provideChunk(int x, int z) {
        return null;
    }

    default Chunk loadChunk(int x, int z) {
        return provideChunk(x, z);
    }

    default void populate(IChunkProvider provider, int x, int z) {
    }

    default boolean unloadQueuedChunks() {
        return false;
    }

    default boolean canSave() {
        return true;
    }

    default void recreateStructures(int x, int z) {
    }

    default void saveExtraData() {
    }

    /** The mobs that may spawn at this position (null = the biome's own list). */
    default java.util.List getPossibleCreatures(legacyconnector.v1_7_10.net.minecraft.entity.EnumCreatureType type, int x, int y, int z) {
        return null;
    }
}
