package legacyconnector.v1_7_10.net.minecraft.world.biome;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import legacyconnector.v1_7_10.net.minecraft.block.Block;
import legacyconnector.v1_7_10.net.minecraft.init.Blocks;
import legacyconnector.v1_7_10.net.minecraft.world.World;

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
        var owner = org.kylepitcock.legacyconnector.v1_7_10.LegacyModContainer1710.active();
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

    public List<SpawnListEntry> getSpawnableList(legacyconnector.v1_7_10.net.minecraft.entity.EnumCreatureType type) {
        return switch (type) {
            case monster -> spawnableMonsterList;
            case creature -> spawnableCreatureList;
            case waterCreature -> spawnableWaterCreatureList;
            default -> spawnableCaveCreatureList;
        };
    }

    /** Forge: the flowers bonemeal and the generator may place in the biome (block, metadata, weight). */
    public final List<Object[]> mcc$flowers = new ArrayList<>();

    public void addFlower(legacyconnector.v1_7_10.net.minecraft.block.Block block, int meta, int weight) {
        mcc$flowers.add(new Object[] {block, meta, weight});
    }

    public void addDefaultFlowers() {
        addFlower(legacyconnector.v1_7_10.net.minecraft.init.Blocks.yellow_flower, 0, 20);
        addFlower(legacyconnector.v1_7_10.net.minecraft.init.Blocks.red_flower, 0, 10);
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

    /** Vanilla's surface pass for one column of freshly generated terrain (mods override it for their own layering). */
    public void genTerrainBlocks(World world, Random rand, Block[] blocks, byte[] meta, int x, int z, double noise) {
        genBiomeTerrain(world, rand, blocks, meta, x, z, noise);
    }

    public final void genBiomeTerrain(World world, Random rand, Block[] blocks, byte[] meta, int x, int z, double noise) {
        Block top = this.topBlock;
        byte topMeta = (byte) (this.field_150604_aj & 255);
        Block filler = this.fillerBlock;
        int depth = -1;
        int layer = (int) (noise / 3.0D + 3.0D + rand.nextDouble() * 0.25D);
        int cx = x & 15;
        int cz = z & 15;
        int height = blocks.length / 256;
        for (int y = height - 1; y >= 0; --y) {
            int i = (cz * 16 + cx) * height + y;
            if (y <= rand.nextInt(5)) {
                blocks[i] = Blocks.bedrock;
                continue;
            }
            Block b = blocks[i];
            if (b == null || b == Blocks.air) {
                depth = -1;
            } else if (b == Blocks.stone) {
                if (depth == -1) {
                    if (layer <= 0) {
                        top = null;
                        topMeta = 0;
                        filler = Blocks.stone;
                    } else if (y >= 59 && y <= 64) {
                        top = this.topBlock;
                        topMeta = (byte) (this.field_150604_aj & 255);
                        filler = this.fillerBlock;
                    }
                    if (y < 63 && (top == null || top == Blocks.air)) {
                        top = getFloatTemperature(x, y, z) < 0.15F ? Blocks.ice : Blocks.water;
                        topMeta = 0;
                    }
                    depth = layer;
                    if (y >= 62) {
                        blocks[i] = top;
                        meta[i] = topMeta;
                    } else if (y < 56 - layer) {
                        top = null;
                        filler = Blocks.stone;
                        blocks[i] = Blocks.gravel;
                    } else {
                        blocks[i] = filler;
                    }
                } else if (depth > 0) {
                    --depth;
                    blocks[i] = filler;
                    if (depth == 0 && filler == Blocks.sand) {
                        depth = rand.nextInt(4) + Math.max(0, y - 63);
                        filler = Blocks.sandstone;
                    }
                }
            }
        }
    }

    public void decorate(World world, Random rand, int chunkX, int chunkZ) {
        theBiomeDecorator.decorateChunk(world, rand, this, chunkX, chunkZ);
    }

    protected static final legacyconnector.v1_7_10.net.minecraft.world.gen.feature.WorldGenDoublePlant genTallFlowers =
            new legacyconnector.v1_7_10.net.minecraft.world.gen.feature.WorldGenDoublePlant();
    protected legacyconnector.v1_7_10.net.minecraft.world.gen.feature.WorldGenTrees worldGeneratorTrees =
            new legacyconnector.v1_7_10.net.minecraft.world.gen.feature.WorldGenTrees(false);
    protected legacyconnector.v1_7_10.net.minecraft.world.gen.feature.WorldGenBigTree worldGeneratorBigTree =
            new legacyconnector.v1_7_10.net.minecraft.world.gen.feature.WorldGenBigTree(false);

    /** The tree generator for one tree of this biome (mods override this to grow their own trees). */
    public legacyconnector.v1_7_10.net.minecraft.world.gen.feature.WorldGenAbstractTree func_150567_a(Random rand) {
        return rand.nextInt(10) == 0 ? worldGeneratorBigTree : worldGeneratorTrees;
    }

    /** The plant generator for one grass patch of this biome. */
    public legacyconnector.v1_7_10.net.minecraft.world.gen.feature.WorldGenerator getRandomWorldGenForGrass(Random rand) {
        return new legacyconnector.v1_7_10.net.minecraft.world.gen.feature.WorldGenTallGrass(
                legacyconnector.v1_7_10.net.minecraft.init.Blocks.tallgrass, 1);
    }

    public int getBiomeGrassColor(int x, int y, int z) {
        return 0x91BD59;
    }

    public int getBiomeFoliageColor(int x, int y, int z) {
        return 0x77AB2F;
    }

    public int getSkyColorByTemp(float temp) {
        temp /= 3.0F;
        temp = Math.max(-1.0F, Math.min(1.0F, temp));
        return java.awt.Color.HSBtoRGB(0.62222224F - temp * 0.05F, 0.5F + temp * 0.1F, 1.0F) & 0xFFFFFF;
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

    /** As in 1.7.10: null for an unused id (mods check it to find free biome ids). */
    public static BiomeGenBase getBiome(int id) {
        return id >= 0 && id < biomeList.length ? biomeList[id] : ocean;
    }

    /** The biome of an id as the world generator uses it: unused ids are oceans. */
    public static BiomeGenBase mcc$biomeOrOcean(int id) {
        return id >= 0 && id < biomeList.length && biomeList[id] != null ? biomeList[id] : ocean;
    }

    /** True for biomes created by legacy mods (generated as modern biomes by Legacy Connector). */
    public boolean mcc$isModBiome() {
        return mcc$owner != null && biomeID > 39 && (biomeID < 128 || biomeID > 167);
    }

    /** Id of the modern biome generated for this legacy biome. */
    public net.minecraft.resources.ResourceLocation mcc$modernId() {
        String name = biomeName == null || biomeName.isBlank() ? "biome_" + biomeID : biomeName;
        return net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(mcc$owner == null ? "legacyconnector" : mcc$owner,
                org.kylepitcock.legacyconnector.core.Ids.normalizePath(name).replace('/', '_'));
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

    public static class SpawnListEntry extends legacyconnector.v1_7_10.net.minecraft.util.WeightedRandom.Item {
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

    public void plantFlower(legacyconnector.v1_7_10.net.minecraft.world.World world, Random rand, int x, int y, int z) {
        if (mcc$flowers.isEmpty()) addDefaultFlowers();
        int total = 0;
        for (Object[] f : mcc$flowers) total += (Integer) f[2];
        int pick = rand.nextInt(Math.max(1, total));
        for (Object[] f : mcc$flowers) {
            pick -= (Integer) f[2];
            if (pick < 0) {
                world.setBlock(x, y, z, (legacyconnector.v1_7_10.net.minecraft.block.Block) f[0], (Integer) f[1], 3);
                return;
            }
        }
    }

    /** The biome with the id; {@code fallback} true returns the ocean for an unknown id instead of null. */
    public static BiomeGenBase func_150557_a(int id, boolean fallback) {
        BiomeGenBase biome = getBiome(id);
        return biome == null && fallback ? mcc$biomeOrOcean(id) : biome;
    }
}
