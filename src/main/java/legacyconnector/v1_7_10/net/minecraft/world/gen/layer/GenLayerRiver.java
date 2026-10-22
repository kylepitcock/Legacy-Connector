package legacyconnector.v1_7_10.net.minecraft.world.gen.layer;

import legacyconnector.v1_7_10.net.minecraft.world.biome.BiomeGenBase;

/** River where the river-init ids change; -1 elsewhere. */
public class GenLayerRiver extends GenLayerNeighbors {
    public GenLayerRiver(long seed, GenLayer parent) {
        super(seed, parent);
    }

    private static int bucket(int v) {
        return v >= 2 ? 2 + (v & 1) : v;
    }

    @Override
    int transform(int c, int n, int e, int w, int s, int nw, int ne, int sw, int se) {
        int k = bucket(c);
        if (k == bucket(n) && k == bucket(e) && k == bucket(w) && k == bucket(s)) return -1;
        return BiomeGenBase.river.biomeID;
    }
}
