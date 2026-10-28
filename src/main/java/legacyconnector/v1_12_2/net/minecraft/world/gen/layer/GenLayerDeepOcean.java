package legacyconnector.v1_12_2.net.minecraft.world.gen.layer;

import legacyconnector.v1_12_2.net.minecraft.init.Biomes;

/** Ocean surrounded by ocean becomes deep ocean (more than two ocean neighbours, as 1.12). */
public class GenLayerDeepOcean extends GenLayerNeighbors {
    public GenLayerDeepOcean(long seed, GenLayer parent) {
        super(seed, parent);
    }

    @Override
    int transform(int c, int n, int e, int w, int s, int nw, int ne, int sw, int se) {
        if (c != 0) return c;
        int oceans = (n == 0 ? 1 : 0) + (e == 0 ? 1 : 0) + (w == 0 ? 1 : 0) + (s == 0 ? 1 : 0);
        return oceans > 3 ? id(Biomes.DEEP_OCEAN) : c;
    }
}
