package legacyconnector.v1_12_2.net.minecraft.world.biome;

/** What a biome adds to a chunk when it is decorated: the counts mods tune per biome. */
public class BiomeDecorator {
    public boolean decorating;
    public int sandPerChunk = 1;
    public int sandPerChunk2 = 3;
    public int clayPerChunk = 1;
    public int treesPerChunk;
    public int flowersPerChunk = 2;
    public int grassPerChunk = 1;
    public int deadBushPerChunk;
    public int mushroomsPerChunk;
    public int reedsPerChunk;
    public int cactiPerChunk;
    public int gravelPatchesPerChunk = 1;
    public int sandPatchesPerChunk = 3;
    public int clayPatchesPerChunk = 1;
    public int bigMushroomsPerChunk;
    public boolean generateFalls = true;
    public int waterlilyPerChunk;

    public java.util.Random decorationRandom;
    public legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos chunkPos;
    public float extraTreeChance = 0.1F;
    public legacyconnector.v1_12_2.net.minecraft.world.gen.feature.WorldGenFlowers flowerGen = new legacyconnector.v1_12_2.net.minecraft.world.gen.feature.WorldGenFlowers(
            legacyconnector.v1_12_2.net.minecraft.block.BlockFlower.EnumFlowerColor.YELLOW.getBlock(),
            legacyconnector.v1_12_2.net.minecraft.block.BlockFlower.EnumFlowerType.DANDELION);
    public legacyconnector.v1_12_2.net.minecraft.world.gen.feature.WorldGenerator mushroomBrownGen =
            mcc$modernPatch(net.minecraft.data.worldgen.features.VegetationFeatures.PATCH_BROWN_MUSHROOM);
    public legacyconnector.v1_12_2.net.minecraft.world.gen.feature.WorldGenerator mushroomRedGen =
            mcc$modernPatch(net.minecraft.data.worldgen.features.VegetationFeatures.PATCH_RED_MUSHROOM);

    /** A plant patch placed as the modern game's feature of the same plant. */
    private static legacyconnector.v1_12_2.net.minecraft.world.gen.feature.WorldGenerator mcc$modernPatch(
            net.minecraft.resources.ResourceKey<net.minecraft.world.level.levelgen.feature.ConfiguredFeature<?, ?>> key) {
        return new legacyconnector.v1_12_2.net.minecraft.world.gen.feature.WorldGenerator() {
            @Override
            public boolean generate(legacyconnector.v1_12_2.net.minecraft.world.World world, java.util.Random rand,
                    legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos) {
                return org.kylepitcock.legacyconnector.v1_12_2.bridge.ModernFeatures1122.place(world, key, rand, pos);
            }
        };
    }
    public legacyconnector.v1_12_2.net.minecraft.world.gen.ChunkGeneratorSettings chunkProviderSettings;
    public legacyconnector.v1_12_2.net.minecraft.world.gen.feature.WorldGenerator clayGen = new legacyconnector.v1_12_2.net.minecraft.world.gen.feature.WorldGenClay(4);
    public legacyconnector.v1_12_2.net.minecraft.world.gen.feature.WorldGenerator sandGen = new legacyconnector.v1_12_2.net.minecraft.world.gen.feature.WorldGenSand(mcc$block("sand"), 7);
    public legacyconnector.v1_12_2.net.minecraft.world.gen.feature.WorldGenerator gravelGen = new legacyconnector.v1_12_2.net.minecraft.world.gen.feature.WorldGenSand(mcc$block("gravel"), 6);
    public legacyconnector.v1_12_2.net.minecraft.world.gen.feature.WorldGenerator bigMushroomGen = new legacyconnector.v1_12_2.net.minecraft.world.gen.feature.WorldGenBigMushroom();
    public legacyconnector.v1_12_2.net.minecraft.world.gen.feature.WorldGenerator reedGen = new legacyconnector.v1_12_2.net.minecraft.world.gen.feature.WorldGenReed();
    public legacyconnector.v1_12_2.net.minecraft.world.gen.feature.WorldGenerator cactusGen = new legacyconnector.v1_12_2.net.minecraft.world.gen.feature.WorldGenCactus();
    public legacyconnector.v1_12_2.net.minecraft.world.gen.feature.WorldGenerator waterlilyGen = new legacyconnector.v1_12_2.net.minecraft.world.gen.feature.WorldGenWaterlily();
    public legacyconnector.v1_12_2.net.minecraft.world.gen.feature.WorldGenerator dirtGen;
    public legacyconnector.v1_12_2.net.minecraft.world.gen.feature.WorldGenerator gravelOreGen;
    public legacyconnector.v1_12_2.net.minecraft.world.gen.feature.WorldGenerator graniteGen;
    public legacyconnector.v1_12_2.net.minecraft.world.gen.feature.WorldGenerator dioriteGen;
    public legacyconnector.v1_12_2.net.minecraft.world.gen.feature.WorldGenerator andesiteGen;
    public legacyconnector.v1_12_2.net.minecraft.world.gen.feature.WorldGenerator coalGen;
    public legacyconnector.v1_12_2.net.minecraft.world.gen.feature.WorldGenerator ironGen;
    public legacyconnector.v1_12_2.net.minecraft.world.gen.feature.WorldGenerator goldGen;
    public legacyconnector.v1_12_2.net.minecraft.world.gen.feature.WorldGenerator redstoneGen;
    public legacyconnector.v1_12_2.net.minecraft.world.gen.feature.WorldGenerator diamondGen;
    public legacyconnector.v1_12_2.net.minecraft.world.gen.feature.WorldGenerator lapisGen;

    private static legacyconnector.v1_12_2.net.minecraft.block.Block mcc$block(String name) {
        try {
            return org.kylepitcock.legacyconnector.v1_12_2.bridge.Registries1122.BLOCKS.getValue(new legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation("minecraft", name));
        } catch (RuntimeException | LinkageError e) {
            return null;
        }
    }

    private static legacyconnector.v1_12_2.net.minecraft.world.gen.feature.WorldGenerator mcc$ore(String block, int size) {
        var b = mcc$block(block);
        return b == null ? null : new legacyconnector.v1_12_2.net.minecraft.world.gen.feature.WorldGenMinable(b.getDefaultState(), size);
    }

    /** Vanilla's ore veins, sized by the generator settings (vanilla defaults when the decorator has none). */
    private void mcc$initOres() {
        if (coalGen != null) return;
        var c = chunkProviderSettings != null ? chunkProviderSettings : new legacyconnector.v1_12_2.net.minecraft.world.gen.ChunkGeneratorSettings.Factory().build();
        if (chunkProviderSettings == null) chunkProviderSettings = c;
        dirtGen = mcc$ore("dirt", c.dirtSize);
        gravelOreGen = mcc$ore("gravel", c.gravelSize);
        graniteGen = mcc$ore("granite", c.graniteSize);
        dioriteGen = mcc$ore("diorite", c.dioriteSize);
        andesiteGen = mcc$ore("andesite", c.andesiteSize);
        coalGen = mcc$ore("coal_ore", c.coalSize);
        ironGen = mcc$ore("iron_ore", c.ironSize);
        goldGen = mcc$ore("gold_ore", c.goldSize);
        redstoneGen = mcc$ore("redstone_ore", c.redstoneSize);
        diamondGen = mcc$ore("diamond_ore", c.diamondSize);
        lapisGen = mcc$ore("lapis_ore", c.lapisSize);
    }

    /** Vanilla's ore pass for the chunk being decorated, with Forge's ore events (Pre, GenerateMinable per ore, Post). */
    protected void generateOres(legacyconnector.v1_12_2.net.minecraft.world.World worldIn, java.util.Random random) {
        mcc$initOres();
        var c = chunkProviderSettings;
        var bus = legacyconnector.v1_12_2.net.minecraftforge.common.MinecraftForge.ORE_GEN_BUS;
        bus.post(new legacyconnector.v1_12_2.net.minecraftforge.event.terraingen.OreGenEvent.Pre(worldIn, random, chunkPos));
        mcc$ore(worldIn, random, dirtGen, "DIRT", 1, c.dirtCount, c.dirtMinHeight, c.dirtMaxHeight);
        mcc$ore(worldIn, random, gravelOreGen, "GRAVEL", 1, c.gravelCount, c.gravelMinHeight, c.gravelMaxHeight);
        mcc$ore(worldIn, random, dioriteGen, "DIORITE", 1, c.dioriteCount, c.dioriteMinHeight, c.dioriteMaxHeight);
        mcc$ore(worldIn, random, graniteGen, "GRANITE", 1, c.graniteCount, c.graniteMinHeight, c.graniteMaxHeight);
        mcc$ore(worldIn, random, andesiteGen, "ANDESITE", 1, c.andesiteCount, c.andesiteMinHeight, c.andesiteMaxHeight);
        mcc$ore(worldIn, random, coalGen, "COAL", 1, c.coalCount, c.coalMinHeight, c.coalMaxHeight);
        mcc$ore(worldIn, random, ironGen, "IRON", 1, c.ironCount, c.ironMinHeight, c.ironMaxHeight);
        mcc$ore(worldIn, random, goldGen, "GOLD", 1, c.goldCount, c.goldMinHeight, c.goldMaxHeight);
        mcc$ore(worldIn, random, redstoneGen, "REDSTONE", 1, c.redstoneCount, c.redstoneMinHeight, c.redstoneMaxHeight);
        mcc$ore(worldIn, random, diamondGen, "DIAMOND", 1, c.diamondCount, c.diamondMinHeight, c.diamondMaxHeight);
        mcc$ore(worldIn, random, lapisGen, "LAPIS", 2, c.lapisCount, c.lapisCenterHeight, c.lapisSpread);
        bus.post(new legacyconnector.v1_12_2.net.minecraftforge.event.terraingen.OreGenEvent.Post(worldIn, random, chunkPos));
    }

    private void mcc$ore(legacyconnector.v1_12_2.net.minecraft.world.World world, java.util.Random random, legacyconnector.v1_12_2.net.minecraft.world.gen.feature.WorldGenerator gen, String type, int kind,
            int count, int a, int b) {
        if (gen == null) return;
        var eventType = legacyconnector.v1_12_2.net.minecraftforge.event.terraingen.OreGenEvent.GenerateMinable.EventType.valueOf(type);
        if (!legacyconnector.v1_12_2.net.minecraftforge.event.terraingen.TerrainGen.generateOre(world, random, gen, chunkPos, eventType)) return;
        if (kind == 1) genStandardOre1(world, random, count, gen, a, b);
        else genStandardOre2(world, random, count, gen, a, b);
    }

    /** {@code blockCount} veins at evenly random heights between minHeight and maxHeight. */
    protected void genStandardOre1(legacyconnector.v1_12_2.net.minecraft.world.World worldIn, java.util.Random random, int blockCount, legacyconnector.v1_12_2.net.minecraft.world.gen.feature.WorldGenerator generator,
            int minHeight, int maxHeight) {
        if (maxHeight < minHeight) {
            int t = minHeight;
            minHeight = maxHeight;
            maxHeight = t;
        } else if (maxHeight == minHeight) {
            if (minHeight < 255) ++maxHeight;
            else --minHeight;
        }
        for (int j = 0; j < blockCount; ++j) {
            var pos = chunkPos.add(random.nextInt(16), random.nextInt(maxHeight - minHeight) + minHeight, random.nextInt(16));
            generator.generate(worldIn, random, pos);
        }
    }

    /** {@code blockCount} veins at heights spread triangularly around centerHeight (lapis). */
    protected void genStandardOre2(legacyconnector.v1_12_2.net.minecraft.world.World worldIn, java.util.Random random, int blockCount, legacyconnector.v1_12_2.net.minecraft.world.gen.feature.WorldGenerator generator,
            int centerHeight, int spread) {
        for (int i = 0; i < blockCount; ++i) {
            var pos = chunkPos.add(random.nextInt(16), random.nextInt(spread) + random.nextInt(spread) + centerHeight - spread, random.nextInt(16));
            generator.generate(worldIn, random, pos);
        }
    }

    public BiomeDecorator() {
    }

    /**
     * Decorates a chunk. Vanilla's ores, trees and plants are the modern game's features, so the base pass only records the chunk
     * (mod decorators extending this class add their own features in {@code genDecorations}).
     */
    public void decorate(legacyconnector.v1_12_2.net.minecraft.world.World worldIn, java.util.Random random, Biome biome,
            legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos) {
        if (decorating) return;
        decorating = true;
        try {
            chunkPos = pos;
            decorationRandom = random;
            genDecorations(biome, worldIn, random);
        } finally {
            decorating = false;
        }
    }

    protected void genDecorations(Biome biomeIn, legacyconnector.v1_12_2.net.minecraft.world.World worldIn, java.util.Random random) {
    }
}
