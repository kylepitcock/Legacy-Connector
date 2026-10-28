package legacyconnector.v1_12_2.net.minecraft.world.gen.layer;

import legacyconnector.v1_12_2.net.minecraft.world.WorldType;
import legacyconnector.v1_12_2.net.minecraft.world.biome.Biome;

/**
 * 1.12.2 biome layer: produces an int map (biome ids, or climate/land codes in early layers) for an area, from its parent layer and
 * a deterministic per-position random (the same LCG as vanilla, so mods' layer stacks give the same maps). Mods build their
 * world shapes by chaining these.
 */
public abstract class GenLayer {
    private static final long MUL = 6364136223846793005L;
    private static final long ADD = 1442695040888963407L;

    private long worldGenSeed;
    protected GenLayer parent;
    private long chunkSeed;
    protected long baseSeed;

    public GenLayer(long seed) {
        this.baseSeed = seed;
        for (int i = 0; i < 3; i++) {
            baseSeed *= baseSeed * MUL + ADD;
            baseSeed += seed;
        }
    }

    /** A biome's 1.12 numeric id (vanilla's ids for the vanilla biomes). */
    protected static int id(Biome biome) {
        return Biome.getIdForBiome(biome);
    }

    /**
     * The vanilla 1.12 overworld biome stack: index 0 is the 1:4 biome map (generation), 1 the per-block map; Forge's
     * InitBiomeGens event may replace them (mod world types call this and pass their own biome layer through the world type).
     */
    public static GenLayer[] initializeAllBiomeGenerators(long seed, WorldType worldType,
            legacyconnector.v1_12_2.net.minecraft.world.gen.ChunkGeneratorSettings settings) {
        GenLayer layer = new GenLayerIsland(1L);
        layer = new GenLayerFuzzyZoom(2000L, layer);
        layer = new GenLayerAddIsland(1L, layer);
        layer = new GenLayerZoom(2001L, layer);
        layer = new GenLayerAddIsland(2L, layer);
        layer = new GenLayerAddIsland(50L, layer);
        layer = new GenLayerAddIsland(70L, layer);
        layer = new GenLayerRemoveTooMuchOcean(2L, layer);
        layer = new GenLayerAddSnow(2L, layer);
        layer = new GenLayerAddIsland(3L, layer);
        layer = new GenLayerEdge(2L, layer, GenLayerEdge.Mode.COOL_WARM);
        layer = new GenLayerEdge(2L, layer, GenLayerEdge.Mode.HEAT_ICE);
        layer = new GenLayerEdge(3L, layer, GenLayerEdge.Mode.SPECIAL);
        layer = new GenLayerZoom(2002L, layer);
        layer = new GenLayerZoom(2003L, layer);
        layer = new GenLayerAddIsland(4L, layer);
        layer = new GenLayerAddMushroomIsland(5L, layer);
        layer = new GenLayerDeepOcean(4L, layer);
        GenLayer land = GenLayerZoom.magnify(1000L, layer, 0);
        int biomeSize = 4;
        int riverSize = biomeSize;
        if (settings != null) {
            biomeSize = settings.biomeSize;
            riverSize = settings.riverSize;
        }
        if (worldType == WorldType.LARGE_BIOMES) biomeSize = 6;
        biomeSize = getModdedBiomeSize(worldType, biomeSize);
        GenLayer riverInit = GenLayerZoom.magnify(1000L, land, 0);
        riverInit = new GenLayerRiverInit(100L, riverInit);
        GenLayer biomes = worldType.getBiomeLayer(seed, land, settings);
        GenLayer hillsInput = GenLayerZoom.magnify(1000L, riverInit, 2);
        GenLayer hills = new GenLayerHills(1000L, biomes, hillsInput);
        GenLayer rivers = GenLayerZoom.magnify(1000L, riverInit, 2);
        rivers = GenLayerZoom.magnify(1000L, rivers, riverSize);
        rivers = new GenLayerRiver(1L, rivers);
        rivers = new GenLayerSmooth(1000L, rivers);
        hills = new GenLayerRareBiome(1001L, hills);
        for (int k = 0; k < biomeSize; ++k) {
            hills = new GenLayerZoom(1000 + k, hills);
            if (k == 0) hills = new GenLayerAddIsland(3L, hills);
            if (k == 1 || biomeSize == 1) hills = new GenLayerShore(1000L, hills);
        }
        GenLayer smooth = new GenLayerSmooth(1000L, hills);
        GenLayer mix = new GenLayerRiverMix(100L, smooth, rivers);
        GenLayer voronoi = new GenLayerVoronoiZoom(10L, mix);
        mix.initWorldGenSeed(seed);
        voronoi.initWorldGenSeed(seed);
        return new GenLayer[] {mix, voronoi, mix};
    }

    /** Forge's WorldTypeEvent.BiomeSize hook: the zoom count of the biome layers (mods' own stacks call it too). */
    public static int getModdedBiomeSize(WorldType worldType, int original) {
        var event = new legacyconnector.v1_12_2.net.minecraftforge.event.terraingen.WorldTypeEvent.BiomeSize(worldType, original);
        legacyconnector.v1_12_2.net.minecraftforge.common.MinecraftForge.TERRAIN_GEN_BUS.post(event);
        return event.getNewSize();
    }

    public void initWorldGenSeed(long seed) {
        this.worldGenSeed = seed;
        if (parent != null) parent.initWorldGenSeed(seed);
        for (int i = 0; i < 3; i++) {
            worldGenSeed *= worldGenSeed * MUL + ADD;
            worldGenSeed += baseSeed;
        }
    }

    public void initChunkSeed(long x, long z) {
        chunkSeed = worldGenSeed;
        chunkSeed *= chunkSeed * MUL + ADD;
        chunkSeed += x;
        chunkSeed *= chunkSeed * MUL + ADD;
        chunkSeed += z;
        chunkSeed *= chunkSeed * MUL + ADD;
        chunkSeed += x;
        chunkSeed *= chunkSeed * MUL + ADD;
        chunkSeed += z;
    }

    protected int nextInt(int bound) {
        int i = (int) ((chunkSeed >> 24) % bound);
        if (i < 0) i += bound;
        chunkSeed *= chunkSeed * MUL + ADD;
        chunkSeed += worldGenSeed;
        return i;
    }

    /** Forge: {@link #nextInt} for a long bound (weighted picks over a large total weight). */
    protected long nextLong(long bound) {
        long l = (chunkSeed >> 24) % bound;
        if (l < 0) l += bound;
        chunkSeed *= chunkSeed * MUL + ADD;
        chunkSeed += worldGenSeed;
        return l;
    }

    public abstract int[] getInts(int x, int z, int width, int depth);

    /** Whether two biome ids are the same biome (the mesa plateaus count as one). */
    protected static boolean biomesEqualOrMesaPlateau(int a, int b) {
        if (a == b) return true;
        Biome ba = Biome.getBiome(a);
        Biome bb = Biome.getBiome(b);
        if (ba == null || bb == null) return false;
        Biome plateau = legacyconnector.v1_12_2.net.minecraft.init.Biomes.MESA_ROCK;
        Biome plateauClear = legacyconnector.v1_12_2.net.minecraft.init.Biomes.MESA_CLEAR_ROCK;
        if ((ba == plateau || ba == plateauClear) && (bb == plateau || bb == plateauClear)) return true;
        return ba == bb || ba.getBiomeClass() == bb.getBiomeClass();
    }

    protected static boolean isBiomeOceanic(int id) {
        Biome b = Biome.getBiome(id);
        return b == legacyconnector.v1_12_2.net.minecraft.init.Biomes.OCEAN || b == legacyconnector.v1_12_2.net.minecraft.init.Biomes.DEEP_OCEAN
                || b == legacyconnector.v1_12_2.net.minecraft.init.Biomes.FROZEN_OCEAN;
    }

    protected int selectRandom(int... choices) {
        return choices[nextInt(choices.length)];
    }

    protected int selectModeOrRandom(int a, int b, int c, int d) {
        if (b == c && c == d) return b;
        if (a == b && a == c) return a;
        if (a == b && a == d) return a;
        if (a == c && a == d) return a;
        if (a == b && c != d) return a;
        if (a == c && b != d) return a;
        if (a == d && b != c) return a;
        if (b == c && a != d) return b;
        if (b == d && a != c) return b;
        if (c == d && a != b) return c;
        return selectRandom(a, b, c, d);
    }
}
