package legacyconnector.v1_12_2.net.minecraft.world.gen.layer;

import legacyconnector.v1_12_2.net.minecraft.init.Biomes;

/** Transitions between incompatible neighbours (hills edges, mesa rims, jungle edges, no swamp next to desert). */
public class GenLayerBiomeEdge extends GenLayerNeighbors {
    public GenLayerBiomeEdge(long seed, GenLayer parent) {
        super(seed, parent);
    }

    private static boolean allSame(int c, int n, int e, int w, int s) {
        return biomesEqualOrMesaPlateau(n, c) && biomesEqualOrMesaPlateau(e, c) && biomesEqualOrMesaPlateau(w, c) && biomesEqualOrMesaPlateau(s, c);
    }

    @Override
    int transform(int c, int n, int e, int w, int s, int nw, int ne, int sw, int se) {
        if (c == id(Biomes.EXTREME_HILLS) && !allSame(c, n, e, w, s)) return id(Biomes.EXTREME_HILLS_EDGE);
        if ((c == id(Biomes.MESA_ROCK) || c == id(Biomes.MESA_CLEAR_ROCK)) && !allSame(c, n, e, w, s)) return id(Biomes.MESA);
        if (c == id(Biomes.REDWOOD_TAIGA) && !allSame(c, n, e, w, s)) return id(Biomes.TAIGA);
        int ice = id(Biomes.ICE_PLAINS);
        if (c == id(Biomes.DESERT) && (n == ice || e == ice || w == ice || s == ice)) return id(Biomes.EXTREME_HILLS_WITH_TREES);
        if (c == id(Biomes.SWAMPLAND)) {
            for (int v : new int[] {n, e, w, s}) {
                if (v == id(Biomes.DESERT) || v == id(Biomes.COLD_TAIGA) || v == ice) return id(Biomes.PLAINS);
                if (v == id(Biomes.JUNGLE)) return id(Biomes.JUNGLE_EDGE);
            }
        }
        return c;
    }
}
