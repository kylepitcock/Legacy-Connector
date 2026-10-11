package mccompat.v1_7_10.net.minecraft.world.biome;

import java.util.Random;
import mccompat.v1_7_10.net.minecraft.world.World;

/** Per-biome decoration: counts mods tune, and the legacy passes (trees, plants) driven by them. */
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
        if (currentWorld != null) throw new RuntimeException("Already decorating!!");
        this.currentWorld = world;
        this.randomGenerator = random;
        this.chunk_X = chunkX;
        this.chunk_Z = chunkZ;
        try {
            genDecorations(biome);
        } finally {
            this.currentWorld = null;
            this.randomGenerator = null;
        }
    }

    /**
     * The legacy decoration passes that shape a biome's look: trees from the biome's own tree generator and plants
     * from its grass generator, each gated by Forge's DecorateBiomeEvent. Ores, sand, clay, flowers, mushrooms and
     * the other small features come from the modern biome definition.
     */
    protected void genDecorations(BiomeGenBase biome) {
        var bus = mccompat.v1_7_10.net.minecraftforge.common.MinecraftForge.EVENT_BUS;
        bus.post(new mccompat.v1_7_10.net.minecraftforge.event.terraingen.DecorateBiomeEvent.Pre(currentWorld, randomGenerator, chunk_X, chunk_Z));
        Random rand = randomGenerator;
        int trees = treesPerChunk;
        if (rand.nextInt(10) == 0) trees++;
        if (trees > 0 && mccompat.v1_7_10.net.minecraftforge.event.terraingen.TerrainGen.decorate(currentWorld, rand, chunk_X, chunk_Z,
                mccompat.v1_7_10.net.minecraftforge.event.terraingen.DecorateBiomeEvent.Decorate.EventType.TREE)) {
            for (int i = 0; i < trees; i++) {
                int x = chunk_X + rand.nextInt(16) + 8;
                int z = chunk_Z + rand.nextInt(16) + 8;
                int y = currentWorld.getHeightValue(x, z);
                var tree = biome.func_150567_a(rand);
                if (tree == null) continue;
                tree.setScale(1.0, 1.0, 1.0);
                if (tree.generate(currentWorld, rand, x, y, z)) tree.func_150524_b(currentWorld, rand, x, y, z);
            }
        }
        if (grassPerChunk > 0 && mccompat.v1_7_10.net.minecraftforge.event.terraingen.TerrainGen.decorate(currentWorld, rand, chunk_X, chunk_Z,
                mccompat.v1_7_10.net.minecraftforge.event.terraingen.DecorateBiomeEvent.Decorate.EventType.GRASS)) {
            for (int i = 0; i < grassPerChunk; i++) {
                int x = chunk_X + rand.nextInt(16) + 8;
                int z = chunk_Z + rand.nextInt(16) + 8;
                int top = currentWorld.getHeightValue(x, z) * 2;
                if (top <= 0) continue;
                int y = rand.nextInt(top);
                var gen = biome.getRandomWorldGenForGrass(rand);
                if (gen != null) gen.generate(currentWorld, rand, x, y, z);
            }
        }
        bus.post(new mccompat.v1_7_10.net.minecraftforge.event.terraingen.DecorateBiomeEvent.Post(currentWorld, randomGenerator, chunk_X, chunk_Z));
    }
}
