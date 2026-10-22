package legacyconnector.v1_7_10.net.minecraft.world.gen;

import java.util.Random;
import legacyconnector.v1_7_10.net.minecraft.block.Block;
import legacyconnector.v1_7_10.net.minecraft.world.World;
import legacyconnector.v1_7_10.net.minecraft.world.chunk.IChunkProvider;

/** Base of chunk-spanning generators (caves, ravines, structures): called once per chunk for the chunks around it. */
public class MapGenBase {
    protected int range = 8;
    protected Random rand = new Random();
    protected World worldObj;

    public void generate(IChunkProvider provider, World world, int chunkX, int chunkZ, Block[] blocks) {
        int k = this.range;
        this.worldObj = world;
        this.rand.setSeed(world.getSeed());
        long l = this.rand.nextLong();
        long i1 = this.rand.nextLong();
        for (int j1 = chunkX - k; j1 <= chunkX + k; ++j1) {
            for (int k1 = chunkZ - k; k1 <= chunkZ + k; ++k1) {
                long l1 = j1 * l;
                long i2 = k1 * i1;
                this.rand.setSeed(l1 ^ i2 ^ world.getSeed());
                this.func_151538_a(world, j1, k1, chunkX, chunkZ, blocks);
            }
        }
    }

    protected void func_151538_a(World world, int x, int z, int chunkX, int chunkZ, Block[] blocks) {
    }
}
