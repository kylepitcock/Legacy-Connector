package legacyconnector.v1_12_2.net.minecraft.world.biome;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import legacyconnector.v1_12_2.net.minecraft.entity.EnumCreatureType;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraftforge.registries.IForgeRegistryEntry;

/** 1.12.2 biome: its properties, surface blocks, decorator and spawn lists (mod biomes are data for the legacy world generators). */
public class Biome extends IForgeRegistryEntry.Impl<Biome> {
    public static final legacyconnector.v1_12_2.net.minecraft.util.registry.RegistryNamespaced<legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation, Biome> REGISTRY =
            org.kylepitcock.legacyconnector.v1_12_2.bridge.Registries1122.view(org.kylepitcock.legacyconnector.v1_12_2.bridge.Registries1122.BIOMES, "plains");

    private final String biomeName;
    private final float baseHeight;
    private final float heightVariation;
    private final float temperature;
    private final float rainfall;
    private final int waterColor;
    private final boolean enableSnow;
    private final boolean enableRain;
    public IBlockState topBlock;
    public IBlockState fillerBlock;
    public BiomeDecorator decorator;
    protected List<SpawnListEntry> spawnableMonsterList = new ArrayList<>();
    protected List<SpawnListEntry> spawnableCreatureList = new ArrayList<>();
    protected List<SpawnListEntry> spawnableWaterCreatureList = new ArrayList<>();
    protected List<SpawnListEntry> spawnableCaveCreatureList = new ArrayList<>();
    private final Map<EnumCreatureType, List<SpawnListEntry>> spawnLists = new EnumMap<>(EnumCreatureType.class);
    private final String baseBiomeRegName;
    /** Forge: the flowers bonemeal and the decorator pick from. */
    protected List<Object> flowers = new ArrayList<>();

    protected static final IBlockState STONE = mcc$state("stone");
    protected static final IBlockState AIR = mcc$state("air");
    protected static final IBlockState BEDROCK = mcc$state("bedrock");
    protected static final IBlockState GRAVEL = mcc$state("gravel");
    protected static final IBlockState RED_SANDSTONE = mcc$state("red_sandstone");
    protected static final IBlockState SANDSTONE = mcc$state("sandstone");
    protected static final IBlockState ICE = mcc$state("ice");
    protected static final IBlockState WATER = mcc$state("water");
    protected static final legacyconnector.v1_12_2.net.minecraft.world.gen.NoiseGeneratorPerlin TEMPERATURE_NOISE =
            new legacyconnector.v1_12_2.net.minecraft.world.gen.NoiseGeneratorPerlin(new java.util.Random(1234L), 1);
    protected static final legacyconnector.v1_12_2.net.minecraft.world.gen.NoiseGeneratorPerlin GRASS_COLOR_NOISE =
            new legacyconnector.v1_12_2.net.minecraft.world.gen.NoiseGeneratorPerlin(new java.util.Random(2345L), 1);

    /** A vanilla block's default state by its 1.12 name (air when it cannot be made yet). */
    private static IBlockState mcc$state(String name) {
        try {
            var block = org.kylepitcock.legacyconnector.v1_12_2.bridge.Vanilla1122.blockByName(name);
            if (block != null) return block.getDefaultState();
        } catch (RuntimeException | LinkageError e) {
            // below
        }
        try {
            return org.kylepitcock.legacyconnector.v1_12_2.bridge.Vanilla1122.airState();
        } catch (RuntimeException | LinkageError e) {
            return null;
        }
    }

    public Biome(BiomeProperties properties) {
        this.biomeName = properties.biomeName;
        this.baseHeight = properties.baseHeight;
        this.heightVariation = properties.heightVariation;
        this.temperature = properties.temperature;
        this.rainfall = properties.rainfall;
        this.waterColor = properties.waterColor;
        this.enableSnow = properties.enableSnow;
        this.enableRain = properties.enableRain;
        this.baseBiomeRegName = properties.baseBiome;
        try {
            this.topBlock = org.kylepitcock.legacyconnector.v1_12_2.bridge.Registries1122.BLOCKS.getValue(new legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation("minecraft", "grass")).getDefaultState();
            this.fillerBlock = org.kylepitcock.legacyconnector.v1_12_2.bridge.Registries1122.BLOCKS.getValue(new legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation("minecraft", "dirt")).getDefaultState();
        } catch (RuntimeException | LinkageError e) {
            // vanilla constants not available yet: mods set their own blocks
        }
        this.decorator = createBiomeDecorator();
        spawnLists.put(EnumCreatureType.MONSTER, spawnableMonsterList);
        spawnLists.put(EnumCreatureType.CREATURE, spawnableCreatureList);
        spawnLists.put(EnumCreatureType.WATER_CREATURE, spawnableWaterCreatureList);
        spawnLists.put(EnumCreatureType.AMBIENT, spawnableCaveCreatureList);
    }

    public BiomeDecorator createBiomeDecorator() {
        return new BiomeDecorator();
    }

    public List<SpawnListEntry> getSpawnableList(EnumCreatureType creatureType) {
        return spawnLists.getOrDefault(creatureType, new ArrayList<>());
    }

    public String getBiomeName() {
        return biomeName;
    }

    public float getBaseHeight() {
        return baseHeight;
    }

    public float getHeightVariation() {
        return heightVariation;
    }

    public float getDefaultTemperature() {
        return temperature;
    }

    public float getRainfall() {
        return rainfall;
    }

    public int getWaterColor() {
        return waterColor;
    }

    public boolean getEnableSnow() {
        return enableSnow;
    }

    public boolean canRain() {
        return enableRain;
    }

    public boolean isSnowyBiome() {
        return enableSnow;
    }

    /**
     * The 1.12 numeric id of a biome: vanilla's own ids for the vanilla biomes (ocean 0, plains 1, ... void 127, mutated 129-167;
     * biome layers and mods compare against them), and for every other biome the first free id below 256 in the order they are
     * first asked for (1.12 kept biome ids in a byte per column).
     */
    public static int getIdForBiome(Biome biome) {
        if (biome == null) return -1;
        Integer id = MCC_IDS.get(biome);
        if (id != null) return id;
        synchronized (MCC_BY_ID) {
            mcc$vanillaIds();
            id = MCC_IDS.get(biome);
            if (id != null) return id;
            int free = -1;
            for (int i = 40; i < 256 && free < 0; i++) if (MCC_BY_ID[i] == null) free = i;
            if (free < 0) return REGISTRY.getIDForObject(biome);
            MCC_BY_ID[free] = biome;
            MCC_IDS.put(biome, free);
            return free;
        }
    }

    public static Biome getBiomeForId(int id) {
        if (id >= 0 && id < 256) {
            Biome b = MCC_BY_ID[id];
            if (b != null) return b;
            synchronized (MCC_BY_ID) {
                if (mcc$vanillaIds()) {
                    b = MCC_BY_ID[id];
                    if (b != null) return b;
                }
            }
            if (id >= 40) {
                // a mod biome nobody numbered yet: number the registered biomes in registration order
                synchronized (MCC_BY_ID) {
                    for (Biome r : REGISTRY) getIdForBiome(r);
                    return MCC_BY_ID[id];
                }
            }
            return null;
        }
        return null;
    }

    private static final Biome[] MCC_BY_ID = new Biome[256];
    private static final java.util.Map<Biome, Integer> MCC_IDS = java.util.Collections.synchronizedMap(new java.util.IdentityHashMap<>());
    private static boolean mcc$numbered;
    private static final String[] MCC_VANILLA = {
            "OCEAN 0", "PLAINS 1", "DESERT 2", "EXTREME_HILLS 3", "FOREST 4", "TAIGA 5", "SWAMPLAND 6", "RIVER 7", "HELL 8", "SKY 9",
            "FROZEN_OCEAN 10", "FROZEN_RIVER 11", "ICE_PLAINS 12", "ICE_MOUNTAINS 13", "MUSHROOM_ISLAND 14", "MUSHROOM_ISLAND_SHORE 15", "BEACH 16",
            "DESERT_HILLS 17", "FOREST_HILLS 18", "TAIGA_HILLS 19", "EXTREME_HILLS_EDGE 20", "JUNGLE 21", "JUNGLE_HILLS 22", "JUNGLE_EDGE 23",
            "DEEP_OCEAN 24", "STONE_BEACH 25", "COLD_BEACH 26", "BIRCH_FOREST 27", "BIRCH_FOREST_HILLS 28", "ROOFED_FOREST 29", "COLD_TAIGA 30",
            "COLD_TAIGA_HILLS 31", "REDWOOD_TAIGA 32", "REDWOOD_TAIGA_HILLS 33", "EXTREME_HILLS_WITH_TREES 34", "SAVANNA 35", "SAVANNA_PLATEAU 36",
            "MESA 37", "MESA_ROCK 38", "MESA_CLEAR_ROCK 39", "VOID 127", "MUTATED_PLAINS 129", "MUTATED_DESERT 130", "MUTATED_EXTREME_HILLS 131",
            "MUTATED_FOREST 132", "MUTATED_TAIGA 133", "MUTATED_SWAMPLAND 134", "MUTATED_ICE_FLATS 140", "MUTATED_JUNGLE 149",
            "MUTATED_JUNGLE_EDGE 151", "MUTATED_BIRCH_FOREST 155", "MUTATED_BIRCH_FOREST_HILLS 156", "MUTATED_ROOFED_FOREST 157",
            "MUTATED_TAIGA_COLD 158", "MUTATED_REDWOOD_TAIGA 160", "MUTATED_REDWOOD_TAIGA_HILLS 161", "MUTATED_EXTREME_HILLS_WITH_TREES 162",
            "MUTATED_SAVANNA 163", "MUTATED_SAVANNA_ROCK 164", "MUTATED_MESA 165", "MUTATED_MESA_ROCK 166", "MUTATED_MESA_CLEAR_ROCK 167"};

    /** Puts the vanilla biomes at their 1.12 ids (once); true when it ran now. */
    private static boolean mcc$vanillaIds() {
        if (mcc$numbered) return false;
        mcc$numbered = true;
        for (String e : MCC_VANILLA) {
            String[] p = e.split(" ");
            try {
                Biome b = (Biome) legacyconnector.v1_12_2.net.minecraft.init.Biomes.class.getField(p[0]).get(null);
                if (b != null && !MCC_IDS.containsKey(b)) {
                    int id = Integer.parseInt(p[1]);
                    MCC_BY_ID[id] = b;
                    MCC_IDS.put(b, id);
                }
            } catch (ReflectiveOperationException | RuntimeException | LinkageError ex) {
                // a constant that cannot be made yet keeps no id
            }
        }
        return true;
    }

    public static Biome getBiome(int id) {
        return getBiomeForId(id);
    }

    // ------------------------------------------------------------------ decoration and terrain

    /** Decorates a chunk: the decorator's own pass (vanilla features are placed by the modern game). */
    public void decorate(legacyconnector.v1_12_2.net.minecraft.world.World worldIn, java.util.Random rand, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos) {
        if (decorator != null) decorator.decorate(worldIn, rand, this, pos);
    }

    public void genTerrainBlocks(legacyconnector.v1_12_2.net.minecraft.world.World worldIn, java.util.Random rand,
            legacyconnector.v1_12_2.net.minecraft.world.chunk.ChunkPrimer chunkPrimerIn, int x, int z, double noiseVal) {
        generateBiomeTerrain(worldIn, rand, chunkPrimerIn, x, z, noiseVal);
    }

    private static boolean is(IBlockState state, String name) {
        if (state == null || state.getBlock() == null) return false;
        var id = state.getBlock().getRegistryName();
        return id != null && "minecraft".equals(id.getNamespace()) && name.equals(id.getPath());
    }

    private static boolean isAir(IBlockState state) {
        return state == null || state.getMaterial() == legacyconnector.v1_12_2.net.minecraft.block.material.Material.AIR;
    }

    /** Vanilla's surface pass over a primer column: top and filler blocks over stone, water or ice below sea level, bedrock floor. */
    public final void generateBiomeTerrain(legacyconnector.v1_12_2.net.minecraft.world.World worldIn, java.util.Random rand,
            legacyconnector.v1_12_2.net.minecraft.world.chunk.ChunkPrimer chunkPrimerIn, int x, int z, double noiseVal) {
        int sea = worldIn.getSeaLevel();
        IBlockState top = topBlock;
        IBlockState filler = fillerBlock;
        int j = -1;
        int k = (int) (noiseVal / 3.0D + 3.0D + rand.nextDouble() * 0.25D);
        int l = x & 15;
        int i1 = z & 15;
        legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos.MutableBlockPos pos = new legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos.MutableBlockPos();
        for (int y = 255; y >= 0; --y) {
            if (y <= rand.nextInt(5)) {
                chunkPrimerIn.setBlockState(i1, y, l, BEDROCK);
                continue;
            }
            IBlockState current = chunkPrimerIn.getBlockState(i1, y, l);
            if (isAir(current)) {
                j = -1;
            } else if (is(current, "stone")) {
                if (j == -1) {
                    if (k <= 0) {
                        top = AIR;
                        filler = STONE;
                    } else if (y >= sea - 4 && y <= sea + 1) {
                        top = topBlock;
                        filler = fillerBlock;
                    }
                    if (y < sea && isAir(top)) top = getTemperature(pos.setPos(x, y, z)) < 0.15F ? ICE : WATER;
                    j = k;
                    if (y >= sea - 1) {
                        chunkPrimerIn.setBlockState(i1, y, l, top);
                    } else if (y < sea - 7 - k) {
                        top = AIR;
                        filler = STONE;
                        chunkPrimerIn.setBlockState(i1, y, l, GRAVEL);
                    } else {
                        chunkPrimerIn.setBlockState(i1, y, l, filler);
                    }
                } else if (j > 0) {
                    --j;
                    chunkPrimerIn.setBlockState(i1, y, l, filler);
                    if (j == 0 && is(filler, "sand") && k > 1) {
                        j = rand.nextInt(4) + Math.max(0, y - 63);
                        filler = SANDSTONE;
                    }
                }
            }
        }
    }

    // ------------------------------------------------------------------ climate and colours

    /** The temperature at a height: it falls above y=64, varied by noise, as in 1.12. */
    public final float getTemperature(legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos) {
        if (pos.getY() > 64) {
            float f = (float) (TEMPERATURE_NOISE.getValue((double) ((float) pos.getX() / 8.0F), (double) ((float) pos.getZ() / 8.0F)) * 4.0D);
            return getDefaultTemperature() - (f + (float) pos.getY() - 64.0F) * 0.05F / 30.0F;
        }
        return getDefaultTemperature();
    }

    public TempCategory getTempCategory() {
        if (getDefaultTemperature() < 0.2D) return TempCategory.COLD;
        return getDefaultTemperature() < 1.0D ? TempCategory.MEDIUM : TempCategory.WARM;
    }

    public boolean isHighHumidity() {
        return getRainfall() > 0.85F;
    }

    public float getSpawningChance() {
        return 0.1F;
    }

    public boolean ignorePlayerSpawnSuitability() {
        return false;
    }

    public int getSkyColorByTemp(float currentTemperature) {
        float f = currentTemperature / 3.0F;
        f = Math.max(-1.0F, Math.min(1.0F, f));
        return java.awt.Color.HSBtoRGB(0.62222224F - f * 0.05F, 0.5F + f * 0.1F, 1.0F) & 0xFFFFFF;
    }

    public int getGrassColorAtPos(legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos) {
        double t = Math.max(0.0D, Math.min(1.0D, getTemperature(pos)));
        double r = Math.max(0.0D, Math.min(1.0D, getRainfall()));
        return getModdedBiomeGrassColor(legacyconnector.v1_12_2.net.minecraft.world.ColorizerGrass.getGrassColor(t, r));
    }

    public int getFoliageColorAtPos(legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos) {
        double t = Math.max(0.0D, Math.min(1.0D, getTemperature(pos)));
        double r = Math.max(0.0D, Math.min(1.0D, getRainfall()));
        return getModdedBiomeFoliageColor(legacyconnector.v1_12_2.net.minecraft.world.ColorizerFoliage.getFoliageColor(t, r));
    }

    public int getModdedBiomeGrassColor(int original) {
        return original;
    }

    public int getModdedBiomeFoliageColor(int original) {
        return original;
    }

    public int getWaterColorMultiplier() {
        return getWaterColor();
    }

    public BiomeDecorator getModdedBiomeDecorator(BiomeDecorator original) {
        return original;
    }

    // ------------------------------------------------------------------ identity

    public Class<? extends Biome> getBiomeClass() {
        return getClass();
    }

    public boolean isMutation() {
        return baseBiomeRegName != null;
    }

    public static Biome getBiome(int id, Biome fallback) {
        Biome b = getBiomeForId(id);
        return b == null ? fallback : b;
    }

    public static Biome getMutationForBiome(Biome biome) {
        return null;
    }

    // ------------------------------------------------------------------ flowers

    public void addFlower(IBlockState state, int weight) {
        flowers.add(new FlowerEntry(state, weight));
    }

    /** Places a flower (bonemeal on grass): a weighted pick from {@link #flowers}, else a dandelion. */
    public void plantFlower(legacyconnector.v1_12_2.net.minecraft.world.World world, java.util.Random rand, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos) {
        List<FlowerEntry> entries = new ArrayList<>();
        for (Object o : flowers) if (o instanceof FlowerEntry f) entries.add(f);
        IBlockState state = entries.isEmpty() ? mcc$state("yellow_flower")
                : legacyconnector.v1_12_2.net.minecraft.util.WeightedRandom.getRandomItem(rand, entries).state;
        if (state != null && world.isAirBlock(pos)) world.setBlockState(pos, state, 3);
    }

    /** Forge's weighted flower entry. */
    public static class FlowerEntry extends legacyconnector.v1_12_2.net.minecraft.util.WeightedRandom.Item {
        public final IBlockState state;

        public FlowerEntry(IBlockState state, int weight) {
            super(weight);
            this.state = state;
        }
    }

    /** Coarse climate class of a biome. */
    public enum TempCategory {
        OCEAN,
        COLD,
        MEDIUM,
        WARM
    }

    /** The properties a biome is built from. */
    public static class BiomeProperties {
        private final String biomeName;
        private float baseHeight = 0.1F;
        private float heightVariation = 0.2F;
        private float temperature = 0.5F;
        private float rainfall = 0.5F;
        private int waterColor = 16777215;
        private boolean enableSnow;
        private boolean enableRain = true;
        private String baseBiome;

        public BiomeProperties(String name) {
            this.biomeName = name;
        }

        public BiomeProperties setTemperature(float temperature) {
            this.temperature = temperature;
            return this;
        }

        public BiomeProperties setRainfall(float rainfall) {
            this.rainfall = rainfall;
            return this;
        }

        public BiomeProperties setBaseHeight(float baseHeight) {
            this.baseHeight = baseHeight;
            return this;
        }

        public BiomeProperties setHeightVariation(float heightVariation) {
            this.heightVariation = heightVariation;
            return this;
        }

        public BiomeProperties setRainDisabled() {
            this.enableRain = false;
            return this;
        }

        public BiomeProperties setSnowEnabled() {
            this.enableSnow = true;
            return this;
        }

        public BiomeProperties setWaterColor(int color) {
            this.waterColor = color;
            return this;
        }

        public BiomeProperties setBaseBiome(String name) {
            this.baseBiome = name;
            return this;
        }
    }

    /** A mob a biome can spawn. */
    public static class SpawnListEntry extends legacyconnector.v1_12_2.net.minecraft.util.WeightedRandom.Item {
        public Class<? extends legacyconnector.v1_12_2.net.minecraft.entity.Entity> entityClass;
        public int minGroupCount;
        public int maxGroupCount;

        public SpawnListEntry(Class<? extends legacyconnector.v1_12_2.net.minecraft.entity.Entity> entityClass, int weight, int min, int max) {
            super(weight);
            this.entityClass = entityClass;
            this.minGroupCount = min;
            this.maxGroupCount = max;
        }
    }

    // ------------------------------------------------------------------ area W: vanilla feature generators

    protected static final legacyconnector.v1_12_2.net.minecraft.world.gen.feature.WorldGenTrees TREE_FEATURE = new legacyconnector.v1_12_2.net.minecraft.world.gen.feature.WorldGenTrees(false);
    protected static final legacyconnector.v1_12_2.net.minecraft.world.gen.feature.WorldGenBigTree BIG_TREE_FEATURE = new legacyconnector.v1_12_2.net.minecraft.world.gen.feature.WorldGenBigTree(false);
    protected static final legacyconnector.v1_12_2.net.minecraft.world.gen.feature.WorldGenDoublePlant DOUBLE_PLANT_GENERATOR = new legacyconnector.v1_12_2.net.minecraft.world.gen.feature.WorldGenDoublePlant();

    /** A random tree generator: big oaks one time in ten, else small oaks (vanilla's default). */
    public legacyconnector.v1_12_2.net.minecraft.world.gen.feature.WorldGenAbstractTree getRandomTreeFeature(java.util.Random rand) {
        return rand.nextInt(10) == 0 ? BIG_TREE_FEATURE : TREE_FEATURE;
    }

    /** The grass generator of this biome: a patch of tall grass (the modern game's grass patch). */
    public legacyconnector.v1_12_2.net.minecraft.world.gen.feature.WorldGenerator getRandomWorldGenForGrass(java.util.Random rand) {
        return new legacyconnector.v1_12_2.net.minecraft.world.gen.feature.WorldGenerator() {
            @Override
            public boolean generate(legacyconnector.v1_12_2.net.minecraft.world.World world, java.util.Random r, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos) {
                return org.kylepitcock.legacyconnector.v1_12_2.bridge.ModernFeatures1122.place(world, net.minecraft.data.worldgen.features.VegetationFeatures.PATCH_GRASS, r, pos);
            }
        };
    }

    /** Vanilla's flower pick: a poppy one time in three, else a dandelion. */
    public legacyconnector.v1_12_2.net.minecraft.block.BlockFlower.EnumFlowerType pickRandomFlower(java.util.Random rand, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos) {
        return rand.nextInt(3) > 0 ? legacyconnector.v1_12_2.net.minecraft.block.BlockFlower.EnumFlowerType.DANDELION : legacyconnector.v1_12_2.net.minecraft.block.BlockFlower.EnumFlowerType.POPPY;
    }
}
