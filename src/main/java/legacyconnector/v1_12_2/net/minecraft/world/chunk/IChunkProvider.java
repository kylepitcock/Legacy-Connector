package legacyconnector.v1_12_2.net.minecraft.world.chunk;

/** The chunks of a world. */
public interface IChunkProvider {
    Chunk getLoadedChunk(int x, int z);

    Chunk provideChunk(int x, int z);

    boolean tick();

    String makeString();

    boolean isChunkGeneratedAt(int x, int z);
}
