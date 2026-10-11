package mccompat.v1_7_10.net.minecraft.world.gen.layer;

/** Rare biome variants (sunflower plains); the mutated biomes are not registered here, so it passes through. */
public class GenLayerRareBiome extends GenLayer {
    public GenLayerRareBiome(long seed, GenLayer parent) {
        super(seed);
        this.parent = parent;
    }

    @Override
    public int[] getInts(int x, int z, int w, int h) {
        return parent.getInts(x, z, w, h);
    }
}
