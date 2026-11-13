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

    /** Decorates a chunk (called by a legacy chunk generator's populate): {@link #genDecorations} with the chunk recorded. */
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

    /**
     * 1.12's decoration pass, run where a legacy chunk generator decorates (mod dimensions): ores, sand, clay and gravel patches, the
     * biome's trees ({@code getRandomTreeFeature}), big mushrooms, flowers, the biome's grass ({@code getRandomWorldGenForGrass}), dead
     * bushes, lily pads, mushrooms, reeds, pumpkins, cacti and springs, each behind Forge's decorate event (mod biomes in their own
     * dimension got no trees or plants while this was empty). Mod decorators override it and usually call it.
     */
    protected void genDecorations(Biome biomeIn, legacyconnector.v1_12_2.net.minecraft.world.World worldIn, java.util.Random random) {
        if (chunkPos == null || worldIn == null || biomeIn == null) return;
        var pos = chunkPos;
        var cp = new legacyconnector.v1_12_2.net.minecraft.util.math.ChunkPos(pos);
        legacyconnector.v1_12_2.net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(
                new legacyconnector.v1_12_2.net.minecraftforge.event.terraingen.DecorateBiomeEvent.Pre(worldIn, random, cp));
        mcc$step("ores", () -> generateOres(worldIn, random));
        mcc$patches(worldIn, random, cp, sandGen, sandPatchesPerChunk, "SAND");
        mcc$patches(worldIn, random, cp, clayGen, clayPerChunk, "CLAY");
        mcc$patches(worldIn, random, cp, gravelGen, gravelPatchesPerChunk, "SAND_PASS2");
        int trees = treesPerChunk;
        if (random.nextFloat() < extraTreeChance) ++trees;
        if (mcc$decorate(worldIn, random, cp, "TREE")) {
            for (int i = 0; i < trees; ++i) {
                int x = random.nextInt(16) + 8;
                int z = random.nextInt(16) + 8;
                mcc$step("trees", () -> {
                    var tree = biomeIn.getRandomTreeFeature(random);
                    if (tree == null) return;
                    tree.setDecorationDefaults();
                    var at = worldIn.getHeight(pos.add(x, 0, z));
                    if (tree.generate(worldIn, random, at)) tree.generateSaplings(worldIn, random, at);
                });
            }
        }
        if (mcc$decorate(worldIn, random, cp, "BIG_SHROOM")) {
            for (int i = 0; i < bigMushroomsPerChunk; ++i) {
                int x = random.nextInt(16) + 8;
                int z = random.nextInt(16) + 8;
                mcc$step("big mushrooms", () -> bigMushroomGen.generate(worldIn, random, worldIn.getHeight(pos.add(x, 0, z))));
            }
        }
        if (mcc$decorate(worldIn, random, cp, "FLOWERS")) {
            for (int i = 0; i < flowersPerChunk; ++i) {
                int x = random.nextInt(16) + 8;
                int z = random.nextInt(16) + 8;
                int top = worldIn.getHeight(pos.add(x, 0, z)).getY() + 32;
                if (top <= 0) continue;
                int y = random.nextInt(top);
                mcc$step("flowers", () -> {
                    var at = pos.add(x, y, z);
                    var type = biomeIn.pickRandomFlower(random, at);
                    if (type == null || type.getBlockType() == null) return;
                    var flower = type.getBlockType().getBlock();
                    if (flower == null) return;
                    flowerGen.setGeneratedBlock(flower, type);
                    flowerGen.generate(worldIn, random, at);
                });
            }
        }
        if (mcc$decorate(worldIn, random, cp, "GRASS")) {
            for (int i = 0; i < grassPerChunk; ++i) {
                int x = random.nextInt(16) + 8;
                int z = random.nextInt(16) + 8;
                int top = worldIn.getHeight(pos.add(x, 0, z)).getY() * 2;
                if (top <= 0) continue;
                int y = random.nextInt(top);
                mcc$step("grass", () -> {
                    var gen = biomeIn.getRandomWorldGenForGrass(random);
                    if (gen != null) gen.generate(worldIn, random, pos.add(x, y, z));
                });
            }
        }
        if (mcc$decorate(worldIn, random, cp, "DEAD_BUSH")) {
            for (int i = 0; i < deadBushPerChunk; ++i) {
                int x = random.nextInt(16) + 8;
                int z = random.nextInt(16) + 8;
                int top = worldIn.getHeight(pos.add(x, 0, z)).getY() * 2;
                if (top <= 0) continue;
                int y = random.nextInt(top);
                mcc$step("dead bushes", () -> new legacyconnector.v1_12_2.net.minecraft.world.gen.feature.WorldGenDeadBush().generate(worldIn, random, pos.add(x, y, z)));
            }
        }
        if (mcc$decorate(worldIn, random, cp, "LILYPAD")) {
            for (int i = 0; i < waterlilyPerChunk; ++i) {
                int x = random.nextInt(16) + 8;
                int z = random.nextInt(16) + 8;
                int top = worldIn.getHeight(pos.add(x, 0, z)).getY() * 2;
                if (top <= 0) continue;
                int y = random.nextInt(top);
                mcc$step("lily pads", () -> {
                    var at = pos.add(x, y, z);
                    while (at.getY() > 0 && worldIn.isAirBlock(at.down())) at = at.down();
                    waterlilyGen.generate(worldIn, random, at);
                });
            }
        }
        if (mcc$decorate(worldIn, random, cp, "SHROOM")) {
            for (int i = 0; i < mushroomsPerChunk; ++i) {
                if (random.nextInt(4) == 0) mcc$plant(worldIn, random, pos, mushroomBrownGen, 0);
                if (random.nextInt(8) == 0) mcc$plant(worldIn, random, pos, mushroomRedGen, 0);
            }
            if (random.nextInt(4) == 0) mcc$plant(worldIn, random, pos, mushroomBrownGen, 2);
            if (random.nextInt(8) == 0) mcc$plant(worldIn, random, pos, mushroomRedGen, 2);
        }
        if (mcc$decorate(worldIn, random, cp, "REED")) {
            for (int i = 0; i < reedsPerChunk + 10; ++i) mcc$plant(worldIn, random, pos, reedGen, 2);
        }
        if (random.nextInt(32) == 0 && mcc$decorate(worldIn, random, cp, "PUMPKIN")) {
            mcc$plant(worldIn, random, pos, new legacyconnector.v1_12_2.net.minecraft.world.gen.feature.WorldGenPumpkin(), 2);
        }
        if (mcc$decorate(worldIn, random, cp, "CACTUS")) {
            for (int i = 0; i < cactiPerChunk; ++i) mcc$plant(worldIn, random, pos, cactusGen, 2);
        }
        if (generateFalls) {
            if (mcc$decorate(worldIn, random, cp, "LAKE_WATER")) {
                var water = new legacyconnector.v1_12_2.net.minecraft.world.gen.feature.WorldGenLiquids(legacyconnector.v1_12_2.net.minecraft.init.Blocks.FLOWING_WATER);
                for (int i = 0; i < 50; ++i) {
                    int x = random.nextInt(16) + 8;
                    int z = random.nextInt(16) + 8;
                    int y = random.nextInt(random.nextInt(248) + 8);
                    if (y > 0) mcc$step("springs", () -> water.generate(worldIn, random, pos.add(x, y, z)));
                }
            }
            if (mcc$decorate(worldIn, random, cp, "LAKE_LAVA")) {
                var lava = new legacyconnector.v1_12_2.net.minecraft.world.gen.feature.WorldGenLiquids(legacyconnector.v1_12_2.net.minecraft.init.Blocks.FLOWING_LAVA);
                for (int i = 0; i < 20; ++i) {
                    int x = random.nextInt(16) + 8;
                    int z = random.nextInt(16) + 8;
                    int y = random.nextInt(random.nextInt(random.nextInt(240) + 8) + 8);
                    mcc$step("springs", () -> lava.generate(worldIn, random, pos.add(x, y, z)));
                }
            }
        }
        legacyconnector.v1_12_2.net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(
                new legacyconnector.v1_12_2.net.minecraftforge.event.terraingen.DecorateBiomeEvent.Post(worldIn, random, cp));
    }

    /** A plant generator at a random column of the chunk, at a random height up to twice the surface ({@code factor} 2) or on it (0). */
    private void mcc$plant(legacyconnector.v1_12_2.net.minecraft.world.World world, java.util.Random random, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos,
            legacyconnector.v1_12_2.net.minecraft.world.gen.feature.WorldGenerator gen, int factor) {
        if (gen == null) return;
        int x = random.nextInt(16) + 8;
        int z = random.nextInt(16) + 8;
        if (factor == 0) {
            mcc$step("plants", () -> gen.generate(world, random, world.getHeight(pos.add(x, 0, z))));
            return;
        }
        int top = world.getHeight(pos.add(x, 0, z)).getY() * factor;
        if (top <= 0) return;
        int y = random.nextInt(top);
        mcc$step("plants", () -> gen.generate(world, random, pos.add(x, y, z)));
    }

    private void mcc$patches(legacyconnector.v1_12_2.net.minecraft.world.World world, java.util.Random random, legacyconnector.v1_12_2.net.minecraft.util.math.ChunkPos cp,
            legacyconnector.v1_12_2.net.minecraft.world.gen.feature.WorldGenerator gen, int count, String type) {
        if (gen == null || count <= 0 || !mcc$decorate(world, random, cp, type)) return;
        for (int i = 0; i < count; ++i) {
            int x = random.nextInt(16) + 8;
            int z = random.nextInt(16) + 8;
            mcc$step("patches", () -> gen.generate(world, random, world.getTopSolidOrLiquidBlock(chunkPos.add(x, 0, z))));
        }
    }

    private static boolean mcc$decorate(legacyconnector.v1_12_2.net.minecraft.world.World world, java.util.Random random,
            legacyconnector.v1_12_2.net.minecraft.util.math.ChunkPos cp, String type) {
        try {
            return legacyconnector.v1_12_2.net.minecraftforge.event.terraingen.TerrainGen.decorate(world, random, cp,
                    legacyconnector.v1_12_2.net.minecraftforge.event.terraingen.DecorateBiomeEvent.Decorate.EventType.valueOf(type));
        } catch (RuntimeException | LinkageError e) {
            return true;
        }
    }

    private static final java.util.concurrent.atomic.AtomicInteger MCC_FAILURES = new java.util.concurrent.atomic.AtomicInteger();

    /** One generator call; generation must never throw (a failure is logged a few times and skipped). */
    private static void mcc$step(String what, Runnable step) {
        try {
            step.run();
        } catch (RuntimeException | LinkageError e) {
            int n = MCC_FAILURES.incrementAndGet();
            if (n <= 3 || n % 1000 == 0) {
                org.slf4j.LoggerFactory.getLogger("LegacyConnector/Decorator1122").warn("1.12.2 biome decoration ({}) failed ({} failures so far): {}", what, n, e.toString());
            }
        }
    }
}
