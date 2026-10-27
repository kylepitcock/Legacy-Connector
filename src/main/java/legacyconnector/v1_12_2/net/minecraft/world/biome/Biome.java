package legacyconnector.v1_12_2.net.minecraft.world.biome;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import legacyconnector.v1_12_2.net.minecraft.entity.EnumCreatureType;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;
import legacyconnector.v1_12_2.net.minecraftforge.registries.IForgeRegistryEntry;

/**
 * 1.12.2 biome: its properties, surface blocks, decorator and spawn lists (mod biomes are data for the legacy world generators).
 * A biome resolved from a modern one ({@code World.getBiome}) answers its climate from that biome ({@link #mcc$bind}).
 */
public class Biome extends IForgeRegistryEntry.Impl<Biome> {
    public static final legacyconnector.v1_12_2.net.minecraft.util.registry.RegistryNamespaced<legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation, Biome> REGISTRY =
            org.kylepitcock.legacyconnector.v1_12_2.bridge.Registries1122.view(org.kylepitcock.legacyconnector.v1_12_2.bridge.Registries1122.BIOMES, "plains");

    protected static final IBlockState STONE = mcc$vanilla("stone");
    protected static final IBlockState AIR = mcc$vanilla("air");
    protected static final IBlockState BEDROCK = mcc$vanilla("bedrock");
    protected static final IBlockState GRAVEL = mcc$vanilla("gravel");
    protected static final IBlockState RED_SANDSTONE = mcc$vanilla("red_sandstone");
    protected static final IBlockState SANDSTONE = mcc$vanilla("sandstone");
    protected static final IBlockState ICE = mcc$vanilla("ice");
    protected static final IBlockState WATER = mcc$vanilla("water");
    protected static final legacyconnector.v1_12_2.net.minecraft.world.gen.NoiseGeneratorPerlin TEMPERATURE_NOISE =
            new legacyconnector.v1_12_2.net.minecraft.world.gen.NoiseGeneratorPerlin(new Random(1234L), 1);
    protected static final legacyconnector.v1_12_2.net.minecraft.world.gen.NoiseGeneratorPerlin GRASS_COLOR_NOISE =
            new legacyconnector.v1_12_2.net.minecraft.world.gen.NoiseGeneratorPerlin(new Random(2345L), 1);

    private final String biomeName;
    private final float baseHeight;
    private final float heightVariation;
    private final float temperature;
    private final float rainfall;
    private final int waterColor;
    private final boolean enableSnow;
    private final boolean enableRain;
    private final String baseBiomeRegName;
    public IBlockState topBlock;
    public IBlockState fillerBlock;
    public BiomeDecorator decorator;
    protected List<SpawnListEntry> spawnableMonsterList = new ArrayList<>();
    protected List<SpawnListEntry> spawnableCreatureList = new ArrayList<>();
    protected List<SpawnListEntry> spawnableWaterCreatureList = new ArrayList<>();
    protected List<SpawnListEntry> spawnableCaveCreatureList = new ArrayList<>();
    protected List<FlowerEntry> flowers = new ArrayList<>();
    private final Map<EnumCreatureType, List<SpawnListEntry>> spawnLists = new EnumMap<>(EnumCreatureType.class);
    /** The modern biome whose climate this one answers with, or null (a legacy mod's own biome keeps its properties). */
    private volatile net.minecraft.world.level.biome.Biome mcc$modern;

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

    private static IBlockState mcc$vanilla(String path) {
        try {
            var block = org.kylepitcock.legacyconnector.v1_12_2.bridge.Registries1122.BLOCKS.getValue(new legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation("minecraft", path));
            return block == null ? null : block.getDefaultState();
        } catch (RuntimeException | LinkageError e) {
            return null;
        }
    }

    /** Answers climate queries from this modern biome from now on. */
    public void mcc$bind(net.minecraft.world.level.biome.Biome modern) {
        if (mcc$modern == null) mcc$modern = modern;
    }

    public net.minecraft.world.level.biome.Biome mcc$modern() {
        return mcc$modern;
    }

    public BiomeDecorator createBiomeDecorator() {
        return getModdedBiomeDecorator(new BiomeDecorator());
    }

    public BiomeDecorator getModdedBiomeDecorator(BiomeDecorator original) {
        return original;
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
        var m = mcc$modern;
        return m != null ? m.getBaseTemperature() : temperature;
    }

    public float getRainfall() {
        var m = mcc$modern;
        return m != null ? m.getModifiedClimateSettings().downfall() : rainfall;
    }

    public int getWaterColor() {
        var m = mcc$modern;
        return m != null ? m.getWaterColor() : waterColor;
    }

    public int getWaterColorMultiplier() {
        return getWaterColor();
    }

    public boolean getEnableSnow() {
        var m = mcc$modern;
        return m != null ? m.getPrecipitationAt(net.minecraft.core.BlockPos.ZERO) == net.minecraft.world.level.biome.Biome.Precipitation.SNOW : enableSnow;
    }

    public boolean canRain() {
        var m = mcc$modern;
        return m != null ? m.hasPrecipitation() && !getEnableSnow() : !enableSnow && enableRain;
    }

    public boolean isSnowyBiome() {
        return getEnableSnow();
    }

    public boolean isHighHumidity() {
        return getRainfall() > 0.85F;
    }

    public float getSpawningChance() {
        return 0.1F;
    }

    public boolean isMutation() {
        return baseBiomeRegName != null;
    }

    public boolean ignorePlayerSpawnSuitability() {
        return false;
    }

    /** The temperature at a position: colder with height above y=64, varied by noise (1.12.2's formula). */
    public final float getTemperature(BlockPos pos) {
        if (pos.getY() > 64) {
            float f = (float) (TEMPERATURE_NOISE.getValue((double) ((float) pos.getX() / 8.0F), (double) ((float) pos.getZ() / 8.0F)) * 4.0D);
            return getDefaultTemperature() - (f + (float) pos.getY() - 64.0F) * 0.05F / 30.0F;
        }
        return getDefaultTemperature();
    }

    public TempCategory getTempCategory() {
        if (getDefaultTemperature() < 0.2F) return TempCategory.COLD;
        return getDefaultTemperature() < 1.0F ? TempCategory.MEDIUM : TempCategory.WARM;
    }

    public Class<? extends Biome> getBiomeClass() {
        return getClass();
    }

    public int getSkyColorByTemp(float currentTemperature) {
        float f = Math.max(-1.0F, Math.min(1.0F, currentTemperature / 3.0F));
        return java.awt.Color.HSBtoRGB(0.62222224F - f * 0.05F, 0.5F + f * 0.1F, 1.0F);
    }

    public int getGrassColorAtPos(BlockPos pos) {
        var m = mcc$modern;
        if (m != null) return getModdedBiomeGrassColor(m.getGrassColor(pos.getX(), pos.getZ()));
        double t = Math.max(0.0F, Math.min(1.0F, getTemperature(pos)));
        double r = Math.max(0.0F, Math.min(1.0F, getRainfall()));
        return getModdedBiomeGrassColor(legacyconnector.v1_12_2.net.minecraft.world.ColorizerGrass.getGrassColor(t, r));
    }

    public int getFoliageColorAtPos(BlockPos pos) {
        var m = mcc$modern;
        if (m != null) return getModdedBiomeFoliageColor(m.getFoliageColor());
        double t = Math.max(0.0F, Math.min(1.0F, getTemperature(pos)));
        double r = Math.max(0.0F, Math.min(1.0F, getRainfall()));
        return getModdedBiomeFoliageColor(legacyconnector.v1_12_2.net.minecraft.world.ColorizerFoliage.getFoliageColor(t, r));
    }

    public int getModdedBiomeGrassColor(int original) {
        return original;
    }

    public int getModdedBiomeFoliageColor(int original) {
        return original;
    }

    public void addFlower(IBlockState state, int weight) {
        flowers.add(new FlowerEntry(state, weight));
    }

    /** Places a random flower of this biome (Forge's flower list, else a dandelion or poppy as vanilla's pickRandomFlower). */
    public void plantFlower(World world, Random rand, BlockPos pos) {
        IBlockState state;
        if (flowers.isEmpty()) {
            var block = org.kylepitcock.legacyconnector.v1_12_2.bridge.Registries1122.BLOCKS.getValue(new legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation("minecraft",
                    rand.nextInt(3) > 0 ? "yellow_flower" : "red_flower"));
            if (block == null) return;
            state = block.getDefaultState();
        } else {
            state = legacyconnector.v1_12_2.net.minecraft.util.WeightedRandom.getRandomItem(rand, flowers).state;
        }
        world.setBlockState(pos, state, 3);
    }

    public void decorate(World world, Random rand, BlockPos pos) {
        decorator.decorate(world, rand, this, pos);
    }

    public void genTerrainBlocks(World world, Random rand, legacyconnector.v1_12_2.net.minecraft.world.chunk.ChunkPrimer primer, int x, int z, double noiseVal) {
        generateBiomeTerrain(world, rand, primer, x, z, noiseVal);
    }

    /** Vanilla's surface pass for one column of a chunk primer: bedrock floor, top and filler blocks, water and ice to sea level. */
    public final void generateBiomeTerrain(World world, Random rand, legacyconnector.v1_12_2.net.minecraft.world.chunk.ChunkPrimer primer, int x, int z,
            double noiseVal) {
        int sea = world.getSeaLevel();
        IBlockState top = topBlock;
        IBlockState filler = fillerBlock;
        int j = -1;
        int k = (int) (noiseVal / 3.0D + 3.0D + rand.nextDouble() * 0.25D);
        int l = x & 15;
        int i1 = z & 15;
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        var stoneBlock = STONE == null ? null : STONE.getBlock();
        var sandBlock = mcc$sand();
        for (int y = 255; y >= 0; --y) {
            if (y <= rand.nextInt(5)) {
                primer.setBlockState(i1, y, l, BEDROCK);
            } else {
                IBlockState current = primer.getBlockState(i1, y, l);
                if (current == null || current.getMaterial() == legacyconnector.v1_12_2.net.minecraft.block.material.Material.AIR) {
                    j = -1;
                } else if (current.getBlock() == stoneBlock) {
                    if (j == -1) {
                        if (k <= 0) {
                            top = AIR;
                            filler = STONE;
                        } else if (y >= sea - 4 && y <= sea + 1) {
                            top = topBlock;
                            filler = fillerBlock;
                        }
                        if (y < sea && (top == null || top.getMaterial() == legacyconnector.v1_12_2.net.minecraft.block.material.Material.AIR)) {
                            top = getTemperature(mutable.setPos(x, y, z)) < 0.15F ? ICE : WATER;
                        }
                        j = k;
                        if (y >= sea - 1) {
                            primer.setBlockState(i1, y, l, top);
                        } else if (y < sea - 7 - k) {
                            top = AIR;
                            filler = STONE;
                            primer.setBlockState(i1, y, l, GRAVEL);
                        } else {
                            primer.setBlockState(i1, y, l, filler);
                        }
                    } else if (j > 0) {
                        --j;
                        primer.setBlockState(i1, y, l, filler);
                        if (j == 0 && filler != null && filler.getBlock() == sandBlock && k > 1) {
                            j = rand.nextInt(4) + Math.max(0, y - 63);
                            filler = mcc$isRedSand(filler) ? RED_SANDSTONE : SANDSTONE;
                        }
                    }
                }
            }
        }
    }

    private static legacyconnector.v1_12_2.net.minecraft.block.Block mcc$sand() {
        var s = mcc$vanilla("sand");
        return s == null ? null : s.getBlock();
    }

    private static boolean mcc$isRedSand(IBlockState state) {
        return org.kylepitcock.legacyconnector.v1_12_2.bridge.StatesBridge1122.toModern(state).is(net.minecraft.world.level.block.Blocks.RED_SAND);
    }

    public static int getIdForBiome(Biome biome) {
        return REGISTRY.getIDForObject(biome);
    }

    public static Biome getBiomeForId(int id) {
        return REGISTRY.getObjectById(id);
    }

    public static Biome getBiome(int id) {
        return getBiome(id, null);
    }

    public static Biome getBiome(int id, Biome fallback) {
        Biome b = getBiomeForId(id);
        return b == null ? fallback : b;
    }

    public static Biome getMutationForBiome(Biome biome) {
        return null;
    }

    public static void registerBiomes() {
        // vanilla biomes are the modern ones (see Biomes1122)
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

        @Override
        public String toString() {
            return (entityClass == null ? "null" : entityClass.getSimpleName()) + "*(" + minGroupCount + "-" + maxGroupCount + "):" + itemWeight;
        }
    }

    /** Forge: a flower a biome's flower generator may place, by weight. */
    public static class FlowerEntry extends legacyconnector.v1_12_2.net.minecraft.util.WeightedRandom.Item {
        public final IBlockState state;

        public FlowerEntry(IBlockState state, int weight) {
            super(weight);
            this.state = state;
        }
    }

    public enum TempCategory {
        OCEAN,
        COLD,
        MEDIUM,
        WARM
    }
}
