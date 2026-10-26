package legacyconnector.v1_7_10.net.minecraft.world.biome;

import java.util.Random;
import legacyconnector.v1_7_10.net.minecraft.world.World;

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

    protected legacyconnector.v1_7_10.net.minecraft.world.gen.feature.WorldGenerator dirtGen = minable(legacyconnector.v1_7_10.net.minecraft.init.Blocks.dirt, 32);
    protected legacyconnector.v1_7_10.net.minecraft.world.gen.feature.WorldGenerator gravelGen = minable(legacyconnector.v1_7_10.net.minecraft.init.Blocks.gravel, 32);
    protected legacyconnector.v1_7_10.net.minecraft.world.gen.feature.WorldGenerator coalGen = minable(legacyconnector.v1_7_10.net.minecraft.init.Blocks.coal_ore, 16);
    protected legacyconnector.v1_7_10.net.minecraft.world.gen.feature.WorldGenerator ironGen = minable(legacyconnector.v1_7_10.net.minecraft.init.Blocks.iron_ore, 8);
    protected legacyconnector.v1_7_10.net.minecraft.world.gen.feature.WorldGenerator goldGen = minable(legacyconnector.v1_7_10.net.minecraft.init.Blocks.gold_ore, 8);
    protected legacyconnector.v1_7_10.net.minecraft.world.gen.feature.WorldGenerator redstoneGen = minable(legacyconnector.v1_7_10.net.minecraft.init.Blocks.redstone_ore, 7);
    protected legacyconnector.v1_7_10.net.minecraft.world.gen.feature.WorldGenerator diamondGen = minable(legacyconnector.v1_7_10.net.minecraft.init.Blocks.diamond_ore, 7);
    protected legacyconnector.v1_7_10.net.minecraft.world.gen.feature.WorldGenerator lapisGen = minable(legacyconnector.v1_7_10.net.minecraft.init.Blocks.lapis_ore, 6);

    private static legacyconnector.v1_7_10.net.minecraft.world.gen.feature.WorldGenerator minable(legacyconnector.v1_7_10.net.minecraft.block.Block block, int size) {
        return new legacyconnector.v1_7_10.net.minecraft.world.gen.feature.WorldGenMinable(block, size);
    }

    /** {@code count} veins at heights spread evenly between the bounds. */
    protected void genStandardOre1(int count, legacyconnector.v1_7_10.net.minecraft.world.gen.feature.WorldGenerator gen, int minY, int maxY) {
        if (maxY <= minY) return;
        for (int i = 0; i < count; i++) {
            int x = chunk_X + randomGenerator.nextInt(16);
            int y = randomGenerator.nextInt(maxY - minY) + minY;
            int z = chunk_Z + randomGenerator.nextInt(16);
            gen.generate(currentWorld, randomGenerator, x, y, z);
        }
    }

    /** {@code count} veins at heights clustered around {@code center} (a triangle of width 2 * spread). */
    protected void genStandardOre2(int count, legacyconnector.v1_7_10.net.minecraft.world.gen.feature.WorldGenerator gen, int center, int spread) {
        if (spread <= 0) return;
        for (int i = 0; i < count; i++) {
            int x = chunk_X + randomGenerator.nextInt(16);
            int y = randomGenerator.nextInt(spread) + randomGenerator.nextInt(spread) + (center - spread);
            int z = chunk_Z + randomGenerator.nextInt(16);
            gen.generate(currentWorld, randomGenerator, x, y, z);
        }
    }

    /**
     * Vanilla's ore pass (dirt, gravel, coal, iron, gold, redstone, diamond and lapis veins), each gated by Forge's ore event. The
     * modern overworld has its own ores; mods call this from their dimensions' decorators.
     */
    protected void generateOres() {
        var bus = legacyconnector.v1_7_10.net.minecraftforge.common.MinecraftForge.ORE_GEN_BUS;
        bus.post(new legacyconnector.v1_7_10.net.minecraftforge.event.terraingen.OreGenEvent.Pre(currentWorld, randomGenerator, chunk_X, chunk_Z));
        if (ore(dirtGen, legacyconnector.v1_7_10.net.minecraftforge.event.terraingen.OreGenEvent.GenerateMinable.EventType.DIRT)) genStandardOre1(20, dirtGen, 0, 256);
        if (ore(gravelGen, legacyconnector.v1_7_10.net.minecraftforge.event.terraingen.OreGenEvent.GenerateMinable.EventType.GRAVEL)) genStandardOre1(10, gravelGen, 0, 256);
        if (ore(coalGen, legacyconnector.v1_7_10.net.minecraftforge.event.terraingen.OreGenEvent.GenerateMinable.EventType.COAL)) genStandardOre1(20, coalGen, 0, 128);
        if (ore(ironGen, legacyconnector.v1_7_10.net.minecraftforge.event.terraingen.OreGenEvent.GenerateMinable.EventType.IRON)) genStandardOre1(20, ironGen, 0, 64);
        if (ore(goldGen, legacyconnector.v1_7_10.net.minecraftforge.event.terraingen.OreGenEvent.GenerateMinable.EventType.GOLD)) genStandardOre1(2, goldGen, 0, 32);
        if (ore(redstoneGen, legacyconnector.v1_7_10.net.minecraftforge.event.terraingen.OreGenEvent.GenerateMinable.EventType.REDSTONE)) genStandardOre1(8, redstoneGen, 0, 16);
        if (ore(diamondGen, legacyconnector.v1_7_10.net.minecraftforge.event.terraingen.OreGenEvent.GenerateMinable.EventType.DIAMOND)) genStandardOre1(1, diamondGen, 0, 16);
        if (ore(lapisGen, legacyconnector.v1_7_10.net.minecraftforge.event.terraingen.OreGenEvent.GenerateMinable.EventType.LAPIS)) genStandardOre2(1, lapisGen, 16, 16);
        bus.post(new legacyconnector.v1_7_10.net.minecraftforge.event.terraingen.OreGenEvent.Post(currentWorld, randomGenerator, chunk_X, chunk_Z));
    }

    private boolean ore(legacyconnector.v1_7_10.net.minecraft.world.gen.feature.WorldGenerator gen,
            legacyconnector.v1_7_10.net.minecraftforge.event.terraingen.OreGenEvent.GenerateMinable.EventType type) {
        return gen != null && legacyconnector.v1_7_10.net.minecraftforge.event.terraingen.TerrainGen.generateOre(currentWorld, randomGenerator, gen, chunk_X, chunk_Z, type);
    }

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
        var bus = legacyconnector.v1_7_10.net.minecraftforge.common.MinecraftForge.EVENT_BUS;
        bus.post(new legacyconnector.v1_7_10.net.minecraftforge.event.terraingen.DecorateBiomeEvent.Pre(currentWorld, randomGenerator, chunk_X, chunk_Z));
        Random rand = randomGenerator;
        int trees = treesPerChunk;
        if (rand.nextInt(10) == 0) trees++;
        if (trees > 0 && legacyconnector.v1_7_10.net.minecraftforge.event.terraingen.TerrainGen.decorate(currentWorld, rand, chunk_X, chunk_Z,
                legacyconnector.v1_7_10.net.minecraftforge.event.terraingen.DecorateBiomeEvent.Decorate.EventType.TREE)) {
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
        if (grassPerChunk > 0 && legacyconnector.v1_7_10.net.minecraftforge.event.terraingen.TerrainGen.decorate(currentWorld, rand, chunk_X, chunk_Z,
                legacyconnector.v1_7_10.net.minecraftforge.event.terraingen.DecorateBiomeEvent.Decorate.EventType.GRASS)) {
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
        bus.post(new legacyconnector.v1_7_10.net.minecraftforge.event.terraingen.DecorateBiomeEvent.Post(currentWorld, randomGenerator, chunk_X, chunk_Z));
    }
}
