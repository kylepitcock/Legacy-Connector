package legacyconnector.v1_12_2.net.minecraft.world.gen.layer;

import legacyconnector.v1_12_2.net.minecraft.init.Biomes;

/** Hilly variants inside larger biomes (one in three, plus where the river noise allows). */
public class GenLayerHills extends GenLayer {
    private final GenLayer riverLayer;

    public GenLayerHills(long seed, GenLayer biomes, GenLayer rivers) {
        super(seed);
        this.parent = biomes;
        this.riverLayer = rivers;
    }

    @Override
    public void initWorldGenSeed(long seed) {
        super.initWorldGenSeed(seed);
        riverLayer.initWorldGenSeed(seed);
    }

    private static int hills(int b) {
        if (b == id(Biomes.DESERT)) return id(Biomes.DESERT_HILLS);
        if (b == id(Biomes.FOREST)) return id(Biomes.FOREST_HILLS);
        if (b == id(Biomes.BIRCH_FOREST)) return id(Biomes.BIRCH_FOREST_HILLS);
        if (b == id(Biomes.ROOFED_FOREST)) return id(Biomes.PLAINS);
        if (b == id(Biomes.TAIGA)) return id(Biomes.TAIGA_HILLS);
        if (b == id(Biomes.REDWOOD_TAIGA)) return id(Biomes.REDWOOD_TAIGA_HILLS);
        if (b == id(Biomes.COLD_TAIGA)) return id(Biomes.COLD_TAIGA_HILLS);
        if (b == id(Biomes.ICE_PLAINS)) return id(Biomes.ICE_MOUNTAINS);
        if (b == id(Biomes.JUNGLE)) return id(Biomes.JUNGLE_HILLS);
        if (b == id(Biomes.OCEAN)) return id(Biomes.DEEP_OCEAN);
        if (b == id(Biomes.EXTREME_HILLS)) return id(Biomes.EXTREME_HILLS_WITH_TREES);
        if (b == id(Biomes.SAVANNA)) return id(Biomes.SAVANNA_PLATEAU);
        if (b == id(Biomes.MESA_ROCK)) return id(Biomes.MESA);
        return b;
    }

    @Override
    public int[] getInts(int x, int z, int w, int h) {
        int pw = w + 2;
        int[] in = parent.getInts(x - 1, z - 1, pw, h + 2);
        int[] river = riverLayer.getInts(x - 1, z - 1, pw, h + 2);
        int[] out = IntCache.getIntCache(w * h);
        for (int i = 0; i < h; i++) {
            for (int j = 0; j < w; j++) {
                initChunkSeed(j + x, i + z);
                int c = in[j + 1 + (i + 1) * pw];
                int r = river[j + 1 + (i + 1) * pw];
                boolean noise = (r - 2) % 29 == 0;
                if (c == id(Biomes.PLAINS) && nextInt(3) == 0) {
                    out[j + i * w] = nextInt(3) == 0 ? id(Biomes.FOREST_HILLS) : id(Biomes.FOREST);
                } else if (nextInt(3) == 0 || noise) {
                    int hill = hills(c);
                    if (noise && hill != c) hill = hill + 128 < 256 && legacyconnector.v1_12_2.net.minecraft.world.biome.Biome.getBiome(hill + 128) != null ? hill + 128 : hill;
                    if (hill != c) {
                        int same = 0;
                        if (biomesEqualOrMesaPlateau(in[j + 1 + i * pw], c)) same++;
                        if (biomesEqualOrMesaPlateau(in[j + 2 + (i + 1) * pw], c)) same++;
                        if (biomesEqualOrMesaPlateau(in[j + (i + 1) * pw], c)) same++;
                        if (biomesEqualOrMesaPlateau(in[j + 1 + (i + 2) * pw], c)) same++;
                        out[j + i * w] = same >= 3 ? hill : c;
                    } else {
                        out[j + i * w] = c;
                    }
                } else {
                    out[j + i * w] = c;
                }
            }
        }
        return out;
    }
}
