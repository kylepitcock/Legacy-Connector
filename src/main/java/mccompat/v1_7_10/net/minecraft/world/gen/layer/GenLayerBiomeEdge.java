package mccompat.v1_7_10.net.minecraft.world.gen.layer;

import mccompat.v1_7_10.net.minecraft.world.biome.BiomeGenBase;

/** Transitions between incompatible neighbours (hills edges, mesa rims, jungle edges, no swamp next to desert). */
public class GenLayerBiomeEdge extends GenLayerNeighbors {
    public GenLayerBiomeEdge(long seed, GenLayer parent) {
        super(seed, parent);
    }

    private static int id(BiomeGenBase b) {
        return b.biomeID;
    }

    @Override
    int transform(int c, int n, int e, int w, int s, int nw, int ne, int sw, int se) {
        if (c == id(BiomeGenBase.extremeHills) && !(compareBiomesById(n, c) && compareBiomesById(e, c) && compareBiomesById(w, c) && compareBiomesById(s, c))) {
            return id(BiomeGenBase.extremeHillsEdge);
        }
        if ((c == id(BiomeGenBase.mesaPlateau_F) || c == id(BiomeGenBase.mesaPlateau))
                && !(compareBiomesById(n, c) && compareBiomesById(e, c) && compareBiomesById(w, c) && compareBiomesById(s, c))) {
            return id(BiomeGenBase.mesa);
        }
        if (c == id(BiomeGenBase.megaTaiga) && !(compareBiomesById(n, c) && compareBiomesById(e, c) && compareBiomesById(w, c) && compareBiomesById(s, c))) {
            return id(BiomeGenBase.taiga);
        }
        if (c == id(BiomeGenBase.desert) && (n == id(BiomeGenBase.icePlains) || e == id(BiomeGenBase.icePlains) || w == id(BiomeGenBase.icePlains) || s == id(BiomeGenBase.icePlains))) {
            return id(BiomeGenBase.extremeHillsPlus);
        }
        if (c == id(BiomeGenBase.swampland)) {
            for (int v : new int[] {n, e, w, s}) {
                if (v == id(BiomeGenBase.desert) || v == id(BiomeGenBase.coldTaiga) || v == id(BiomeGenBase.icePlains)) return id(BiomeGenBase.plains);
                if (v == id(BiomeGenBase.jungle)) return id(BiomeGenBase.jungleEdge);
            }
        }
        return c;
    }
}
