package legacyconnector.v1_12_2.net.minecraft.world.gen;

import java.util.Random;
import legacyconnector.v1_12_2.net.minecraft.world.World;
import legacyconnector.v1_12_2.net.minecraft.world.chunk.ChunkPrimer;

/** 1.12.2 base of chunk-spanning carvers and structures: called for every chunk within {@link #range} of the one being generated. */
public class MapGenBase {
    protected int range = 8;
    protected Random rand = new Random();
    protected World world;

    public MapGenBase() {
    }

    public void generate(World worldIn, int x, int z, ChunkPrimer primer) {
        int i = this.range;
        this.world = worldIn;
        this.rand.setSeed(worldIn.getSeed());
        long j = this.rand.nextLong();
        long k = this.rand.nextLong();
        for (int l = x - i; l <= x + i; ++l) {
            for (int i1 = z - i; i1 <= z + i; ++i1) {
                long j1 = (long) l * j;
                long k1 = (long) i1 * k;
                this.rand.setSeed(j1 ^ k1 ^ worldIn.getSeed());
                this.recursiveGenerate(worldIn, l, i1, x, z, primer);
            }
        }
    }

    public static void setupChunkSeed(long seed, Random rand, int x, int z) {
        rand.setSeed(seed);
        long i = rand.nextLong();
        long j = rand.nextLong();
        long k = (long) x * i;
        long l = (long) z * j;
        rand.setSeed(k ^ l ^ seed);
    }

    protected void recursiveGenerate(World worldIn, int chunkX, int chunkZ, int originalX, int originalZ, ChunkPrimer chunkPrimerIn) {
    }
}
