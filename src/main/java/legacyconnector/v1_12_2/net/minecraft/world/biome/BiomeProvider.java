package legacyconnector.v1_12_2.net.minecraft.world.biome;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;

/**
 * 1.12.2 biome source. The provider of a modern world answers from the world's own biome source (the biomes the modern generator
 * places, as their 1.12 counterparts); a mod's own subclass overrides what it computes itself.
 */
public class BiomeProvider {
    public static List<Biome> allowedBiomes = new ArrayList<>();

    private final legacyconnector.v1_12_2.net.minecraft.world.World world;
    private final List<Biome> biomesToSpawnIn = new ArrayList<>();
    /** The 1:4 and per-block biome layers of a mod's layer-based provider (null: answered from the world or the subclass). */
    protected legacyconnector.v1_12_2.net.minecraft.world.gen.layer.GenLayer genBiomes;
    protected legacyconnector.v1_12_2.net.minecraft.world.gen.layer.GenLayer biomeIndexLayer;
    protected final BiomeCache biomeCache = new BiomeCache(this);
    /** The customized world settings the layers were built with (null for other world types). */
    private legacyconnector.v1_12_2.net.minecraft.world.gen.ChunkGeneratorSettings settings;

    protected BiomeProvider() {
        this.world = null;
        biomesToSpawnIn.addAll(allowedBiomes);
    }

    /** A layer-based provider of a world type, as 1.12 built it (mod world types extend this). */
    public BiomeProvider(legacyconnector.v1_12_2.net.minecraft.world.storage.WorldInfo info) {
        this(info == null ? 0L : info.getSeed(), info == null ? legacyconnector.v1_12_2.net.minecraft.world.WorldType.DEFAULT : info.getTerrainType(),
                info == null ? "" : info.getGeneratorOptions());
    }

    private BiomeProvider(long seed, legacyconnector.v1_12_2.net.minecraft.world.WorldType worldType, String options) {
        this();
        try {
            var settings = worldType == legacyconnector.v1_12_2.net.minecraft.world.WorldType.CUSTOMIZED && options != null && !options.isEmpty()
                    ? legacyconnector.v1_12_2.net.minecraft.world.gen.ChunkGeneratorSettings.Factory.jsonToFactory(options).build() : null;
            this.settings = settings;
            var type = worldType == null ? legacyconnector.v1_12_2.net.minecraft.world.WorldType.DEFAULT : worldType;
            var layers = legacyconnector.v1_12_2.net.minecraft.world.gen.layer.GenLayer.initializeAllBiomeGenerators(seed, type, settings);
            layers = getModdedBiomeGenerators(type, seed, layers);
            this.genBiomes = layers[0];
            this.biomeIndexLayer = layers[1];
        } catch (RuntimeException | LinkageError e) {
            // no layers: answered like a provider without a world (plains)
        }
    }

    private BiomeProvider(legacyconnector.v1_12_2.net.minecraft.world.World world) {
        this.world = world;
        biomesToSpawnIn.addAll(allowedBiomes);
    }

    /** The provider of a world (its biomes come from the modern generator). */
    public static BiomeProvider mcc$of(legacyconnector.v1_12_2.net.minecraft.world.World world) {
        return new BiomeProvider(world);
    }

    public List<Biome> getBiomesToSpawnIn() {
        return biomesToSpawnIn;
    }

    public Biome getBiome(BlockPos pos) {
        return getBiome(pos, null);
    }

    public Biome getBiome(BlockPos pos, Biome defaultBiome) {
        if (world == null && biomeIndexLayer != null) return biomeCache.getBiome(pos.getX(), pos.getZ(), defaultBiome);
        Biome b = world == null ? null : world.mcc$generatorBiome(pos.getX(), world.getSeaLevel(), pos.getZ());
        return b != null ? b : defaultBiome != null ? defaultBiome : legacyconnector.v1_12_2.net.minecraft.init.Biomes.PLAINS;
    }

    public float getTemperatureAtHeight(float temperature, int height) {
        return temperature;
    }

    public Biome[] getBiomesForGeneration(Biome[] biomes, int x, int z, int width, int height) {
        // the 1:4 scale grid generators sample terrain with
        if (biomes == null || biomes.length < width * height) biomes = new Biome[width * height];
        if (world == null && genBiomes != null) {
            int[] ids = genBiomes.getInts(x, z, width, height);
            for (int i = 0; i < width * height; i++) biomes[i] = Biome.getBiome(ids[i], legacyconnector.v1_12_2.net.minecraft.init.Biomes.DEFAULT);
            return biomes;
        }
        for (int j = 0; j < height; j++) {
            for (int i = 0; i < width; i++) biomes[j * width + i] = getBiome(new BlockPos((x + i) << 2, 0, (z + j) << 2));
        }
        return biomes;
    }

    public Biome[] getBiomes(Biome[] oldBiomeList, int x, int z, int width, int depth) {
        return getBiomes(oldBiomeList, x, z, width, depth, true);
    }

    public Biome[] getBiomes(Biome[] listToReuse, int x, int z, int width, int length, boolean cacheFlag) {
        if (listToReuse == null || listToReuse.length < width * length) listToReuse = new Biome[width * length];
        if (world == null && biomeIndexLayer != null) {
            if (cacheFlag && width == 16 && length == 16 && (x & 15) == 0 && (z & 15) == 0) {
                System.arraycopy(biomeCache.getCachedBiomes(x, z), 0, listToReuse, 0, width * length);
                return listToReuse;
            }
            int[] ids = biomeIndexLayer.getInts(x, z, width, length);
            for (int i = 0; i < width * length; i++) listToReuse[i] = Biome.getBiome(ids[i], legacyconnector.v1_12_2.net.minecraft.init.Biomes.DEFAULT);
            return listToReuse;
        }
        for (int j = 0; j < length; j++) {
            for (int i = 0; i < width; i++) listToReuse[j * width + i] = getBiome(new BlockPos(x + i, 0, z + j));
        }
        return listToReuse;
    }

    public boolean areBiomesViable(int x, int z, int radius, List<Biome> allowed) {
        int step = Math.max(1, radius / 4);
        for (int i = x - radius; i <= x + radius; i += step) {
            for (int j = z - radius; j <= z + radius; j += step) {
                if (!allowed.contains(getBiome(new BlockPos(i, 0, j)))) return false;
            }
        }
        return true;
    }

    public BlockPos findBiomePosition(int x, int z, int range, List<Biome> biomes, Random random) {
        BlockPos found = null;
        int count = 0;
        for (int i = x - range; i <= x + range; i += 4) {
            for (int j = z - range; j <= z + range; j += 4) {
                if (biomes.contains(getBiome(new BlockPos(i, 0, j))) && (found == null || random.nextInt(count + 1) == 0)) {
                    found = new BlockPos(i, 0, j);
                    count++;
                }
            }
        }
        return found;
    }

    public void cleanupCache() {
        biomeCache.cleanupCache();
    }

    /** Forge: lets mods replace the biome layers of a world type (WorldTypeEvent.InitBiomeGens on the terrain bus). */
    public legacyconnector.v1_12_2.net.minecraft.world.gen.layer.GenLayer[] getModdedBiomeGenerators(legacyconnector.v1_12_2.net.minecraft.world.WorldType worldType,
            long seed, legacyconnector.v1_12_2.net.minecraft.world.gen.layer.GenLayer[] original) {
        var event = new legacyconnector.v1_12_2.net.minecraftforge.event.terraingen.WorldTypeEvent.InitBiomeGens(worldType, seed, original);
        legacyconnector.v1_12_2.net.minecraftforge.common.MinecraftForge.TERRAIN_GEN_BUS.post(event);
        return event.getNewBiomeGens();
    }

    public boolean isFixedBiome() {
        return false;
    }

    public Biome getFixedBiome() {
        return null;
    }
}
