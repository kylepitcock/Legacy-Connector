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

    public Biome(BiomeProperties properties) {
        this.biomeName = properties.biomeName;
        this.baseHeight = properties.baseHeight;
        this.heightVariation = properties.heightVariation;
        this.temperature = properties.temperature;
        this.rainfall = properties.rainfall;
        this.waterColor = properties.waterColor;
        this.enableSnow = properties.enableSnow;
        this.enableRain = properties.enableRain;
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

    public static int getIdForBiome(Biome biome) {
        return REGISTRY.getIDForObject(biome);
    }

    public static Biome getBiomeForId(int id) {
        return REGISTRY.getObjectById(id);
    }

    public static Biome getBiome(int id) {
        return getBiomeForId(id);
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
}
