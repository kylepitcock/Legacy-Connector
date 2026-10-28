package legacyconnector.v1_12_2.net.minecraft.world.gen.layer;

import legacyconnector.v1_12_2.net.minecraft.init.Biomes;

/** Beaches, stone beaches, cold beaches and mushroom shores where land meets ocean. */
public class GenLayerShore extends GenLayerNeighbors {
    public GenLayerShore(long seed, GenLayer parent) {
        super(seed, parent);
    }

    @Override
    int transform(int c, int n, int e, int w, int s, int nw, int ne, int sw, int se) {
        boolean oceanNear = isBiomeOceanic(n) || isBiomeOceanic(e) || isBiomeOceanic(w) || isBiomeOceanic(s);
        if (c == id(Biomes.MUSHROOM_ISLAND)) {
            int ocean = id(Biomes.OCEAN);
            return n == ocean || e == ocean || w == ocean || s == ocean ? id(Biomes.MUSHROOM_ISLAND_SHORE) : c;
        }
        if (!oceanNear || isBiomeOceanic(c) || c == id(Biomes.RIVER) || c == id(Biomes.SWAMPLAND)) return c;
        if (c == id(Biomes.EXTREME_HILLS) || c == id(Biomes.EXTREME_HILLS_WITH_TREES) || c == id(Biomes.EXTREME_HILLS_EDGE)) return id(Biomes.STONE_BEACH);
        var b = legacyconnector.v1_12_2.net.minecraft.world.biome.Biome.getBiome(c);
        if (b != null && b.isSnowyBiome()) return id(Biomes.COLD_BEACH);
        if (c == id(Biomes.MESA) || c == id(Biomes.MESA_ROCK) || c == id(Biomes.MESA_CLEAR_ROCK)) return c;
        if (c == id(Biomes.JUNGLE) || c == id(Biomes.JUNGLE_HILLS) || c == id(Biomes.JUNGLE_EDGE)) return id(Biomes.BEACH);
        return id(Biomes.BEACH);
    }
}
