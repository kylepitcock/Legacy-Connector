package mccompat.v1_7_10.net.minecraft.world.chunk;

/** Only passed through to IWorldGenerator; legacy chunk generation itself is not emulated. */
public interface IChunkProvider {
    boolean chunkExists(int x, int z);

    String makeString();

    int getLoadedChunkCount();
}
