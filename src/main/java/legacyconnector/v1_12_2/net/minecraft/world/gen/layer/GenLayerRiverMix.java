package legacyconnector.v1_12_2.net.minecraft.world.gen.layer;

import legacyconnector.v1_12_2.net.minecraft.init.Biomes;

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
        int river = id(Biomes.RIVER);
        for (int i = 0; i < w * h; i++) {
            int b = biomes[i];
            if (isBiomeOceanic(b)) out[i] = b;
            else if (rivers[i] == river) {
                if (b == id(Biomes.ICE_PLAINS)) out[i] = id(Biomes.FROZEN_RIVER);
                else if (b == id(Biomes.MUSHROOM_ISLAND) || b == id(Biomes.MUSHROOM_ISLAND_SHORE)) out[i] = id(Biomes.MUSHROOM_ISLAND_SHORE);
                else out[i] = rivers[i] & 255;
            } else out[i] = b;
        }
        return out;
    }
}
