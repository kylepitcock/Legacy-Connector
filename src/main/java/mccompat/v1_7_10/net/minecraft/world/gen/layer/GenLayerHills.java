package mccompat.v1_7_10.net.minecraft.world.gen.layer;

import mccompat.v1_7_10.net.minecraft.world.biome.BiomeGenBase;

/** Hilly variants inside larger biomes (one in three, plus where the river noise allows). */
public class GenLayerHills extends GenLayer {
    private final GenLayer rivers;

    public GenLayerHills(long seed, GenLayer biomes, GenLayer rivers) {
        super(seed);
        this.parent = biomes;
        this.rivers = rivers;
    }

    @Override
    public void initWorldGenSeed(long seed) {
        super.initWorldGenSeed(seed);
        rivers.initWorldGenSeed(seed);
    }

    private static int hills(int b) {
        if (b == BiomeGenBase.desert.biomeID) return BiomeGenBase.desertHills.biomeID;
        if (b == BiomeGenBase.forest.biomeID) return BiomeGenBase.forestHills.biomeID;
        if (b == BiomeGenBase.birchForest.biomeID) return BiomeGenBase.birchForestHills.biomeID;
        if (b == BiomeGenBase.roofedForest.biomeID) return BiomeGenBase.plains.biomeID;
        if (b == BiomeGenBase.taiga.biomeID) return BiomeGenBase.taigaHills.biomeID;
        if (b == BiomeGenBase.megaTaiga.biomeID) return BiomeGenBase.megaTaigaHills.biomeID;
        if (b == BiomeGenBase.coldTaiga.biomeID) return BiomeGenBase.coldTaigaHills.biomeID;
        if (b == BiomeGenBase.icePlains.biomeID) return BiomeGenBase.iceMountains.biomeID;
        if (b == BiomeGenBase.jungle.biomeID) return BiomeGenBase.jungleHills.biomeID;
        if (b == BiomeGenBase.ocean.biomeID) return BiomeGenBase.deepOcean.biomeID;
        if (b == BiomeGenBase.extremeHills.biomeID) return BiomeGenBase.extremeHillsPlus.biomeID;
        if (b == BiomeGenBase.savanna.biomeID) return BiomeGenBase.savannaPlateau.biomeID;
        if (b == BiomeGenBase.mesaPlateau_F.biomeID) return BiomeGenBase.mesa.biomeID;
        return b;
    }

    @Override
    public int[] getInts(int x, int z, int w, int h) {
        int pw = w + 2;
        int[] in = parent.getInts(x - 1, z - 1, pw, h + 2);
        int[] river = rivers.getInts(x - 1, z - 1, pw, h + 2);
        int[] out = IntCache.getIntCache(w * h);
        for (int i = 0; i < h; i++) {
            for (int j = 0; j < w; j++) {
                initChunkSeed(j + x, i + z);
                int c = in[j + 1 + (i + 1) * pw];
                int r = river[j + 1 + (i + 1) * pw];
                boolean noise = (r - 2) % 29 == 0;
                if (c == BiomeGenBase.plains.biomeID && nextInt(3) == 0) {
                    out[j + i * w] = nextInt(3) == 0 ? BiomeGenBase.forestHills.biomeID : BiomeGenBase.forest.biomeID;
                } else if (nextInt(3) == 0 || noise) {
                    int hill = hills(c);
                    if (hill != c) {
                        int same = 0;
                        if (compareBiomesById(in[j + 1 + i * pw], c)) same++;
                        if (compareBiomesById(in[j + 2 + (i + 1) * pw], c)) same++;
                        if (compareBiomesById(in[j + (i + 1) * pw], c)) same++;
                        if (compareBiomesById(in[j + 1 + (i + 2) * pw], c)) same++;
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
