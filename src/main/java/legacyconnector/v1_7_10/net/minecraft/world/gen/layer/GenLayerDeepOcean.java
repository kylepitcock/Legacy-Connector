package legacyconnector.v1_7_10.net.minecraft.world.gen.layer;

import legacyconnector.v1_7_10.net.minecraft.world.biome.BiomeGenBase;

/** Ocean surrounded by ocean becomes deep ocean. */
public class GenLayerDeepOcean extends GenLayerNeighbors {
    public GenLayerDeepOcean(long seed, GenLayer parent) {
        super(seed, parent);
    }

    @Override
    int transform(int c, int n, int e, int w, int s, int nw, int ne, int sw, int se) {
        if (c == 0 && n == 0 && e == 0 && w == 0 && s == 0) return BiomeGenBase.deepOcean.biomeID;
        return c;
    }
}
