package mccompat.v1_7_10.net.minecraft.world.gen.layer;

import mccompat.v1_7_10.net.minecraft.world.WorldType;
import mccompat.v1_7_10.net.minecraft.world.biome.BiomeGenBase;

/**
 * 1.7.10 biome layer: produces an int map (biome ids, or climate/land codes in early layers) for an area, from its
 * parent layer and a deterministic per-position random. Mods build their world shapes by chaining these.
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

    /** The standard 1.7.10 overworld stack; index 0 is the 1:4 biome map, 1 the per-block map. */
    public static GenLayer[] initializeAllBiomeGenerators(long seed, WorldType worldType) {
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
        int size = worldType == WorldType.LARGE_BIOMES ? 6 : 4;
        size = getModdedBiomeSize(worldType, size);
        GenLayer riverInit = new GenLayerRiverInit(100L, GenLayerZoom.magnify(1000L, land, 0));
        GenLayer biomes = worldType.getBiomeLayer(seed, land);
        GenLayer hills = new GenLayerHills(1000L, biomes, GenLayerZoom.magnify(1000L, riverInit, 2));
        GenLayer rivers = GenLayerZoom.magnify(1000L, GenLayerZoom.magnify(1000L, riverInit, 2), size);
        GenLayer river = new GenLayerSmooth(1000L, new GenLayerRiver(1L, rivers));
        GenLayer chain = new GenLayerRareBiome(1001L, hills);
        for (int i = 0; i < size; i++) {
            chain = new GenLayerZoom(1000 + i, chain);
            if (i == 0) chain = new GenLayerAddIsland(3L, chain);
            if (i == 1) chain = new GenLayerShore(1000L, chain);
        }
        GenLayerRiverMix mix = new GenLayerRiverMix(100L, new GenLayerSmooth(1000L, chain), river);
        GenLayer voronoi = new GenLayerVoronoiZoom(10L, mix);
        mix.initWorldGenSeed(seed);
        voronoi.initWorldGenSeed(seed);
        return new GenLayer[] {mix, voronoi, mix};
    }

    /** Forge's WorldTypeEvent.BiomeSize hook; legacy handlers are not consulted. */
    public static int getModdedBiomeSize(WorldType worldType, int original) {
        return original;
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

    public abstract int[] getInts(int x, int z, int width, int depth);

    protected static boolean compareBiomesById(int a, int b) {
        if (a == b) return true;
        int plateauF = BiomeGenBase.mesaPlateau_F.biomeID;
        int plateau = BiomeGenBase.mesaPlateau.biomeID;
        if (a == plateauF || a == plateau) return b == plateauF || b == plateau;
        BiomeGenBase ba = biome(a);
        BiomeGenBase bb = biome(b);
        return ba != null && bb != null && ba.isEqualTo(bb);
    }

    protected static boolean isBiomeOceanic(int id) {
        return id == BiomeGenBase.ocean.biomeID || id == BiomeGenBase.deepOcean.biomeID || id == BiomeGenBase.frozenOcean.biomeID;
    }

    static BiomeGenBase biome(int id) {
        BiomeGenBase[] list = BiomeGenBase.getBiomeGenArray();
        return id >= 0 && id < list.length ? list[id] : null;
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
