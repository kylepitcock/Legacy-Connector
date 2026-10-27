package legacyconnector.v1_12_2.net.minecraft.world.gen.layer;

import legacyconnector.v1_12_2.net.minecraft.init.Biomes;

/** Rare mushroom islands in open ocean (code 0). */
public class GenLayerAddMushroomIsland extends GenLayerNeighbors {
    public GenLayerAddMushroomIsland(long seed, GenLayer parent) {
        super(seed, parent);
    }

    @Override
    int transform(int c, int n, int e, int w, int s, int nw, int ne, int sw, int se) {
        if (c == 0 && nw == 0 && ne == 0 && sw == 0 && se == 0 && nextInt(100) == 0) return id(Biomes.MUSHROOM_ISLAND);
        return c;
    }
}
