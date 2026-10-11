package mccompat.v1_7_10.net.minecraft.world.biome;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import mccompat.v1_7_10.net.minecraft.block.Block;
import mccompat.v1_7_10.net.minecraft.init.Blocks;
import mccompat.v1_7_10.net.minecraft.world.World;

/**
 * Legacy biome. Vanilla biomes keep their 1.7.10 ids, names and climate; mod biomes register themselves in
 * {@link #getBiomeGenArray()} like before. Mod biomes are not generated in the modern world (yet); their data and
 * logic are available to the mods that query them.
 */
public class BiomeGenBase {
    private static final BiomeGenBase[] biomeList = new BiomeGenBase[256];
    public static final Set<BiomeGenBase> explorationBiomesList = new HashSet<>();
    public static final Height height_Default = new Height(0.1F, 0.2F);
    public static final Height height_ShallowWaters = new Height(-0.5F, 0.0F);
    public static final Height height_Oceans = new Height(-1.0F, 0.1F);
    public static final Height height_DeepOceans = new Height(-1.8F, 0.1F);
    public static final Height height_LowPlains = new Height(0.125F, 0.05F);
    public static final Height height_MidPlains = new Height(0.2F, 0.2F);
    public static final Height height_LowHills = new Height(0.45F, 0.3F);
    public static final Height height_HighPlateaus = new Height(1.5F, 0.025F);
    public static final Height height_MidHills = new Height(1.0F, 0.5F);
    public static final Height height_Shores = new Height(0.0F, 0.025F);
    public static final Height height_RockyWaters = new Height(0.1F, 0.8F);
    public static final Height height_LowIslands = new Height(0.2F, 0.3F);
    public static final Height height_PartiallySubmerged = new Height(-0.2F, 0.1F);
    public static final BiomeGenBase ocean = new BiomeGenBase(0).setBiomeName("Ocean").setTemperatureRainfall(0.5F, 0.5F);
    public static final BiomeGenBase plains = new BiomeGenBase(1).setBiomeName("Plains").setTemperatureRainfall(0.8F, 0.4F);
    public static final BiomeGenBase desert = new BiomeGenBase(2).setBiomeName("Desert").setTemperatureRainfall(2.0F, 0.0F).setDisableRain();
    public static final BiomeGenBase extremeHills = new BiomeGenBase(3).setBiomeName("Extreme Hills").setTemperatureRainfall(0.2F, 0.3F);
    public static final BiomeGenBase forest = new BiomeGenBase(4).setBiomeName("Forest").setTemperatureRainfall(0.7F, 0.8F);
    public static final BiomeGenBase taiga = new BiomeGenBase(5).setBiomeName("Taiga").setTemperatureRainfall(0.25F, 0.8F);
    public static final BiomeGenBase swampland = new BiomeGenBase(6).setBiomeName("Swampland").setTemperatureRainfall(0.8F, 0.9F);
    public static final BiomeGenBase river = new BiomeGenBase(7).setBiomeName("River").setTemperatureRainfall(0.5F, 0.5F);
    public static final BiomeGenBase hell = new BiomeGenBase(8).setBiomeName("Hell").setTemperatureRainfall(2.0F, 0.0F).setDisableRain();
    public static final BiomeGenBase sky = new BiomeGenBase(9).setBiomeName("The End").setTemperatureRainfall(0.5F, 0.5F).setDisableRain();
    public static final BiomeGenBase frozenOcean = new BiomeGenBase(10).setBiomeName("FrozenOcean").setTemperatureRainfall(0.0F, 0.5F).setEnableSnow();
    public static final BiomeGenBase frozenRiver = new BiomeGenBase(11).setBiomeName("FrozenRiver").setTemperatureRainfall(0.0F, 0.5F).setEnableSnow();
    public static final BiomeGenBase icePlains = new BiomeGenBase(12).setBiomeName("Ice Plains").setTemperatureRainfall(0.0F, 0.5F).setEnableSnow();
    public static final BiomeGenBase iceMountains = new BiomeGenBase(13).setBiomeName("Ice Mountains").setTemperatureRainfall(0.0F, 0.5F).setEnableSnow();
    public static final BiomeGenBase mushroomIsland = new BiomeGenBase(14).setBiomeName("MushroomIsland").setTemperatureRainfall(0.9F, 1.0F);
    public static final BiomeGenBase mushroomIslandShore = new BiomeGenBase(15).setBiomeName("MushroomIslandShore").setTemperatureRainfall(0.9F, 1.0F);
    public static final BiomeGenBase beach = new BiomeGenBase(16).setBiomeName("Beach").setTemperatureRainfall(0.8F, 0.4F);
    public static final BiomeGenBase desertHills = new BiomeGenBase(17).setBiomeName("DesertHills").setTemperatureRainfall(2.0F, 0.0F).setDisableRain();
    public static final BiomeGenBase forestHills = new BiomeGenBase(18).setBiomeName("ForestHills").setTemperatureRainfall(0.7F, 0.8F);
    public static final BiomeGenBase taigaHills = new BiomeGenBase(19).setBiomeName("TaigaHills").setTemperatureRainfall(0.25F, 0.8F);
    public static final BiomeGenBase extremeHillsEdge = new BiomeGenBase(20).setBiomeName("Extreme Hills Edge").setTemperatureRainfall(0.2F, 0.3F);
    public static final BiomeGenBase jungle = new BiomeGenBase(21).setBiomeName("Jungle").setTemperatureRainfall(0.95F, 0.9F);
    public static final BiomeGenBase jungleHills = new BiomeGenBase(22).setBiomeName("JungleHills").setTemperatureRainfall(0.95F, 0.9F);
    public static final BiomeGenBase jungleEdge = new BiomeGenBase(23).setBiomeName("JungleEdge").setTemperatureRainfall(0.95F, 0.8F);
    public static final BiomeGenBase deepOcean = new BiomeGenBase(24).setBiomeName("Deep Ocean").setTemperatureRainfall(0.5F, 0.5F);
    public static final BiomeGenBase stoneBeach = new BiomeGenBase(25).setBiomeName("Stone Beach").setTemperatureRainfall(0.2F, 0.3F);
    public static final BiomeGenBase coldBeach = new BiomeGenBase(26).setBiomeName("Cold Beach").setTemperatureRainfall(0.05F, 0.3F).setEnableSnow();
    public static final BiomeGenBase birchForest = new BiomeGenBase(27).setBiomeName("Birch Forest").setTemperatureRainfall(0.6F, 0.6F);
    public static final BiomeGenBase birchForestHills = new BiomeGenBase(28).setBiomeName("Birch Forest Hills").setTemperatureRainfall(0.6F, 0.6F);
    public static final BiomeGenBase roofedForest = new BiomeGenBase(29).setBiomeName("Roofed Forest").setTemperatureRainfall(0.7F, 0.8F);
    public static final BiomeGenBase coldTaiga = new BiomeGenBase(30).setBiomeName("Cold Taiga").setTemperatureRainfall(-0.5F, 0.4F).setEnableSnow();
    public static final BiomeGenBase coldTaigaHills = new BiomeGenBase(31).setBiomeName("Cold Taiga Hills").setTemperatureRainfall(-0.5F, 0.4F).setEnableSnow();
    public static final BiomeGenBase megaTaiga = new BiomeGenBase(32).setBiomeName("Mega Taiga").setTemperatureRainfall(0.3F, 0.8F);
    public static final BiomeGenBase megaTaigaHills = new BiomeGenBase(33).setBiomeName("Mega Taiga Hills").setTemperatureRainfall(0.3F, 0.8F);
    public static final BiomeGenBase extremeHillsPlus = new BiomeGenBase(34).setBiomeName("Extreme Hills+").setTemperatureRainfall(0.2F, 0.3F);
    public static final BiomeGenBase savanna = new BiomeGenBase(35).setBiomeName("Savanna").setTemperatureRainfall(1.2F, 0.0F).setDisableRain();
    public static final BiomeGenBase savannaPlateau = new BiomeGenBase(36).setBiomeName("Savanna Plateau").setTemperatureRainfall(1.0F, 0.0F).setDisableRain();
    public static final BiomeGenBase mesa = new BiomeGenBase(37).setBiomeName("Mesa").setTemperatureRainfall(2.0F, 0.0F).setDisableRain();
    public static final BiomeGenBase mesaPlateau_F = new BiomeGenBase(38).setBiomeName("Mesa Plateau F").setTemperatureRainfall(2.0F, 0.0F).setDisableRain();
    public static final BiomeGenBase mesaPlateau = new BiomeGenBase(39).setBiomeName("Mesa Plateau").setTemperatureRainfall(2.0F, 0.0F).setDisableRain();

    public String biomeName;
    public int color;
    public int field_150609_ah;
    public Block topBlock = Blocks.grass;
    public int field_150604_aj;
    public Block fillerBlock = Blocks.dirt;
    public int field_76754_C = 5169201;
    public float rootHeight;
    public float heightVariation;
    public float temperature;
    public float rainfall;
    public int waterColorMultiplier = 16777215;
    public BiomeDecorator theBiomeDecorator;
    protected List<SpawnListEntry> spawnableMonsterList = new ArrayList<>();
    protected List<SpawnListEntry> spawnableCreatureList = new ArrayList<>();
    protected List<SpawnListEntry> spawnableWaterCreatureList = new ArrayList<>();
    protected List<SpawnListEntry> spawnableCaveCreatureList = new ArrayList<>();
    protected boolean enableSnow;
    protected boolean enableRain = true;
    public final int biomeID;
    /** Modern id of the legacy mod that created this biome (null for the vanilla biomes). */
    public final String mcc$owner;

    public BiomeGenBase(int id) {
        this(id, true);
    }

    public BiomeGenBase(int id, boolean register) {
        this.biomeID = id;
        var owner = org.kylepitcock.mccompat.v1_7_10.LegacyModContainer1710.active();
        this.mcc$owner = owner == null ? null : owner.record().modernId;
        this.rootHeight = height_Default.rootHeight;
        this.heightVariation = height_Default.variation;
        this.temperature = 0.5F;
        this.rainfall = 0.5F;
        this.theBiomeDecorator = createBiomeDecorator();
        if (register && id >= 0 && id < biomeList.length) biomeList[id] = this;
    }

    public BiomeDecorator createBiomeDecorator() {
        return new BiomeDecorator();
    }

    public BiomeGenBase setTemperatureRainfall(float temperature, float rainfall) {
        this.temperature = temperature;
        this.rainfall = rainfall;
        return this;
    }

    public final BiomeGenBase setHeight(Height height) {
        this.rootHeight = height.rootHeight;
        this.heightVariation = height.variation;
        return this;
    }

    public BiomeGenBase setDisableRain() {
        this.enableRain = false;
        return this;
    }

    public BiomeGenBase setEnableSnow() {
        this.enableSnow = true;
        return this;
    }

    public BiomeGenBase setBiomeName(String name) {
        this.biomeName = name;
        return this;
    }

    public BiomeGenBase setColor(int color) {
        this.color = color;
        return this;
    }

    public BiomeGenBase func_76733_a(int color) {
        this.field_76754_C = color;
        return this;
    }

    public BiomeGenBase createMutation() {
        return null;
    }

    public boolean getEnableSnow() {
        return enableSnow;
    }

    public boolean canSpawnLightningBolt() {
        return !enableSnow && enableRain;
    }

    public boolean isHighHumidity() {
        return rainfall > 0.85F;
    }

    public float getSpawningChance() {
        return 0.1F;
    }

    public final int getIntRainfall() {
        return (int) (rainfall * 65536.0F);
    }

    public final float getFloatRainfall() {
        return rainfall;
    }

    public final float getFloatTemperature(int x, int y, int z) {
        return y > 64 ? temperature - (y - 64) * 0.05F / 30.0F : temperature;
    }

    public void decorate(World world, Random rand, int chunkX, int chunkZ) {
        theBiomeDecorator.decorateChunk(world, rand, this, chunkX, chunkZ);
    }

    protected mccompat.v1_7_10.net.minecraft.world.gen.feature.WorldGenTrees worldGeneratorTrees =
            new mccompat.v1_7_10.net.minecraft.world.gen.feature.WorldGenTrees(false);
    protected mccompat.v1_7_10.net.minecraft.world.gen.feature.WorldGenBigTree worldGeneratorBigTree =
            new mccompat.v1_7_10.net.minecraft.world.gen.feature.WorldGenBigTree(false);

    /** The tree generator for one tree of this biome (mods override this to grow their own trees). */
    public mccompat.v1_7_10.net.minecraft.world.gen.feature.WorldGenAbstractTree func_150567_a(Random rand) {
        return rand.nextInt(10) == 0 ? worldGeneratorBigTree : worldGeneratorTrees;
    }

    /** The plant generator for one grass patch of this biome. */
    public mccompat.v1_7_10.net.minecraft.world.gen.feature.WorldGenerator getRandomWorldGenForGrass(Random rand) {
        return new mccompat.v1_7_10.net.minecraft.world.gen.feature.WorldGenTallGrass(
                mccompat.v1_7_10.net.minecraft.init.Blocks.tallgrass, 1);
    }

    public int getBiomeGrassColor(int x, int y, int z) {
        return 0x91BD59;
    }

    public int getBiomeFoliageColor(int x, int y, int z) {
        return 0x77AB2F;
    }

    public int getSkyColorByTemp(float temp) {
        return 0x78A7FF;
    }

    public boolean isEqualTo(BiomeGenBase other) {
        return other == this || other != null && getBiomeClass() == other.getBiomeClass();
    }

    public Class<? extends BiomeGenBase> getBiomeClass() {
        return getClass();
    }

    public TempCategory getTempCategory() {
        if (temperature < 0.2F) return TempCategory.COLD;
        if (temperature < 1.0F) return TempCategory.MEDIUM;
        return TempCategory.WARM;
    }

    public static BiomeGenBase[] getBiomeGenArray() {
        return biomeList;
    }

    public static BiomeGenBase getBiome(int id) {
        return id >= 0 && id < biomeList.length && biomeList[id] != null ? biomeList[id] : ocean;
    }

    /** True for biomes created by legacy mods (generated as modern biomes by Retrofit). */
    public boolean mcc$isModBiome() {
        return mcc$owner != null && biomeID > 39 && (biomeID < 128 || biomeID > 167);
    }

    /** Id of the modern biome generated for this legacy biome. */
    public net.minecraft.resources.ResourceLocation mcc$modernId() {
        String name = biomeName == null || biomeName.isBlank() ? "biome_" + biomeID : biomeName;
        return net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(mcc$owner == null ? "retrofit" : mcc$owner,
                org.kylepitcock.mccompat.core.Ids.normalizePath(name).replace('/', '_'));
    }

    @Override
    public String toString() {
        return "LegacyBiome{" + biomeID + " " + biomeName + "}";
    }

    public static class Height {
        public float rootHeight;
        public float variation;

        public Height(float rootHeight, float variation) {
            this.rootHeight = rootHeight;
            this.variation = variation;
        }

        public Height attenuate() {
            return new Height(rootHeight * 0.8F, variation * 0.6F);
        }
    }

    public static class SpawnListEntry extends mccompat.v1_7_10.net.minecraft.util.WeightedRandom.Item {
        public Class<?> entityClass;
        public int minGroupCount;
        public int maxGroupCount;

        public SpawnListEntry(Class<?> entityClass, int weight, int min, int max) {
            super(weight);
            this.entityClass = entityClass;
            this.minGroupCount = min;
            this.maxGroupCount = max;
        }

        @Override
        public String toString() {
            return entityClass.getSimpleName() + "*(" + minGroupCount + "-" + maxGroupCount + "):" + itemWeight;
        }
    }

    public enum TempCategory {
        OCEAN, COLD, MEDIUM, WARM
    }
}
