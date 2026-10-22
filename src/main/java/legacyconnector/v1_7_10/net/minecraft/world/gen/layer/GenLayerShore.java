package legacyconnector.v1_7_10.net.minecraft.world.gen.layer;

import legacyconnector.v1_7_10.net.minecraft.world.biome.BiomeGenBase;

/** Beaches, stone beaches and mushroom shores where land meets ocean. */
public class GenLayerShore extends GenLayerNeighbors {
    public GenLayerShore(long seed, GenLayer parent) {
        super(seed, parent);
    }

    @Override
    int transform(int c, int n, int e, int w, int s, int nw, int ne, int sw, int se) {
        boolean oceanNear = isBiomeOceanic(n) || isBiomeOceanic(e) || isBiomeOceanic(w) || isBiomeOceanic(s);
        if (c == BiomeGenBase.mushroomIsland.biomeID) {
            return n == BiomeGenBase.ocean.biomeID || e == BiomeGenBase.ocean.biomeID || w == BiomeGenBase.ocean.biomeID || s == BiomeGenBase.ocean.biomeID
                    ? BiomeGenBase.mushroomIslandShore.biomeID : c;
        }
        if (!oceanNear || isBiomeOceanic(c) || c == BiomeGenBase.river.biomeID || c == BiomeGenBase.swampland.biomeID) return c;
        if (c == BiomeGenBase.extremeHills.biomeID || c == BiomeGenBase.extremeHillsPlus.biomeID || c == BiomeGenBase.extremeHillsEdge.biomeID) {
            return BiomeGenBase.stoneBeach.biomeID;
        }
        BiomeGenBase b = biome(c);
        if (b != null && b.getEnableSnow()) return BiomeGenBase.coldBeach.biomeID;
        if (c == BiomeGenBase.mesa.biomeID || c == BiomeGenBase.mesaPlateau.biomeID || c == BiomeGenBase.mesaPlateau_F.biomeID) return c;
        return BiomeGenBase.beach.biomeID;
    }
}
