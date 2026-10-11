package mccompat.v1_7_10.net.minecraft.world.biome;

import java.util.Random;
import mccompat.v1_7_10.net.minecraft.world.World;

/** Per-biome decoration counts. Mods tune these fields; decoration itself is done by the modern biome. */
public class BiomeDecorator {
    public World currentWorld;
    public Random randomGenerator;
    public int chunk_X;
    public int chunk_Z;
    public int waterlilyPerChunk;
    public int treesPerChunk;
    public int flowersPerChunk = 2;
    public int grassPerChunk = 1;
    public int deadBushPerChunk;
    public int mushroomsPerChunk;
    public int reedsPerChunk;
    public int cactiPerChunk;
    public int sandPerChunk = 1;
    public int sandPerChunk2 = 3;
    public int clayPerChunk = 1;
    public int bigMushroomsPerChunk;
    public boolean generateLakes = true;

    public void decorateChunk(World world, Random random, BiomeGenBase biome, int chunkX, int chunkZ) {
        this.currentWorld = world;
        this.randomGenerator = random;
        this.chunk_X = chunkX;
        this.chunk_Z = chunkZ;
    }
}
