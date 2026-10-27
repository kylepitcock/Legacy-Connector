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
