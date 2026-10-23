package legacyconnector.v1_7_10.net.minecraft.world.gen;

import java.util.Random;
import legacyconnector.v1_7_10.net.minecraft.block.Block;
import legacyconnector.v1_7_10.net.minecraft.init.Blocks;
import legacyconnector.v1_7_10.net.minecraft.world.World;
import legacyconnector.v1_7_10.net.minecraft.world.biome.BiomeGenBase;
import legacyconnector.v1_7_10.net.minecraft.world.chunk.Chunk;
import legacyconnector.v1_7_10.net.minecraft.world.chunk.IChunkProvider;

/**
 * Stand-in for vanilla's overworld chunk provider, handed to mods that ask their world type for the default generator
 * ({@code terrainType.getChunkGenerator}): smooth rolling terrain of stone, with each biome's filler and top blocks.
 */
public class ChunkProviderDefault implements IChunkProvider {
    private final World world;
    private final long seed;

    public ChunkProviderDefault(World world, long seed) {
        this.world = world;
        this.seed = seed;
    }

    private double lattice(int x, int z) {
        long h = x * 341873128712L + z * 132897987541L + seed;
        h = (h ^ (h >>> 29)) * 0xBF58476D1CE4E5B9L;
        h = (h ^ (h >>> 32)) * 0x94D049BB133111EBL;
        return ((h >>> 11) & 0xFFFF) / 65535.0;
    }

    private double noise(double x, double z, int scale) {
        double fx = x / scale, fz = z / scale;
        int ix = (int) Math.floor(fx), iz = (int) Math.floor(fz);
        double tx = fx - ix, tz = fz - iz;
        tx = tx * tx * (3 - 2 * tx);
        tz = tz * tz * (3 - 2 * tz);
        double a = lattice(ix, iz), b = lattice(ix + 1, iz), c = lattice(ix, iz + 1), d = lattice(ix + 1, iz + 1);
        return (a + (b - a) * tx) * (1 - tz) + (c + (d - c) * tx) * tz;
    }

    @Override
    public Chunk provideChunk(int cx, int cz) {
        Block[] blocks = new Block[16 * 16 * 256];
        byte[] meta = new byte[16 * 16 * 256];
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int wx = cx * 16 + x, wz = cz * 16 + z;
                double n = noise(wx, wz, 64) * 0.6 + noise(wx, wz, 24) * 0.3 + noise(wx, wz, 8) * 0.1;
                int height = 58 + (int) (n * 24);
                BiomeGenBase biome = null;
                try {
                    biome = world.getBiomeGenForCoords(wx, wz);
                } catch (RuntimeException ignored) {
                }
                Block top = biome != null && biome.topBlock != null ? biome.topBlock : Blocks.grass;
                Block filler = biome != null && biome.fillerBlock != null ? biome.fillerBlock : Blocks.dirt;
                int column = (x * 16 + z) * 256;
                for (int y = 0; y <= height && y < 256; y++) {
                    blocks[column | y] = y == 0 ? Blocks.bedrock : y == height ? (height < 62 ? filler : top) : y >= height - 3 ? filler : Blocks.stone;
                }
                for (int y = height + 1; y <= 62; y++) blocks[column | y] = Blocks.water;
            }
        }
        return new Chunk(world, blocks, meta, cx, cz);
    }

    @Override
    public boolean chunkExists(int x, int z) {
        return true;
    }

    @Override
    public String makeString() {
        return "ChunkProviderDefault";
    }

    @Override
    public int getLoadedChunkCount() {
        return 0;
    }
}
