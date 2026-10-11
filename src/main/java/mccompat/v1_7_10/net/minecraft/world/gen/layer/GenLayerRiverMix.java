package mccompat.v1_7_10.net.minecraft.world.gen.layer;

import mccompat.v1_7_10.net.minecraft.world.biome.BiomeGenBase;

/** Lays the river map over the biome map (frozen rivers in icy biomes, shores in mushroom islands). */
public class GenLayerRiverMix extends GenLayer {
    private final GenLayer biomePatternGeneratorChain;
    private final GenLayer riverPatternGeneratorChain;

    public GenLayerRiverMix(long seed, GenLayer biomes, GenLayer rivers) {
        super(seed);
        this.biomePatternGeneratorChain = biomes;
        this.riverPatternGeneratorChain = rivers;
    }

    @Override
    public void initWorldGenSeed(long seed) {
        biomePatternGeneratorChain.initWorldGenSeed(seed);
        riverPatternGeneratorChain.initWorldGenSeed(seed);
        super.initWorldGenSeed(seed);
    }

    @Override
    public int[] getInts(int x, int z, int w, int h) {
        int[] biomes = biomePatternGeneratorChain.getInts(x, z, w, h);
        int[] rivers = riverPatternGeneratorChain.getInts(x, z, w, h);
        int[] out = IntCache.getIntCache(w * h);
        for (int i = 0; i < w * h; i++) {
            int b = biomes[i];
            if (isBiomeOceanic(b)) out[i] = b;
            else if (rivers[i] == BiomeGenBase.river.biomeID) {
                if (b == BiomeGenBase.icePlains.biomeID) out[i] = BiomeGenBase.frozenRiver.biomeID;
                else if (b == BiomeGenBase.mushroomIsland.biomeID || b == BiomeGenBase.mushroomIslandShore.biomeID) out[i] = BiomeGenBase.mushroomIslandShore.biomeID;
                else out[i] = rivers[i] & 255;
            } else out[i] = b;
        }
        return out;
    }
}
