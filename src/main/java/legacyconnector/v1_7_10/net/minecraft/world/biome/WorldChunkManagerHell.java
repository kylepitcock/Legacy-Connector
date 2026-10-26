package legacyconnector.v1_7_10.net.minecraft.world.biome;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/** A chunk manager with a single biome everywhere (the nether, the end, and mods' pocket dimensions). */
public class WorldChunkManagerHell extends WorldChunkManager {
    private final BiomeGenBase biome;
    private final float rainfall;

    public WorldChunkManagerHell(BiomeGenBase biome, float rainfall) {
        super();
        this.biome = biome;
        this.rainfall = rainfall;
    }

    @Override
    public BiomeGenBase getBiomeGenAt(int x, int z) {
        return biome;
    }

    @Override
    public BiomeGenBase[] getBiomesForGeneration(BiomeGenBase[] out, int x, int z, int w, int h) {
        return fill(out, w * h);
    }

    @Override
    public BiomeGenBase[] loadBlockGeneratorData(BiomeGenBase[] out, int x, int z, int w, int h) {
        return fill(out, w * h);
    }

    @Override
    public BiomeGenBase[] getBiomeGenAt(BiomeGenBase[] out, int x, int z, int w, int h, boolean cache) {
        return fill(out, w * h);
    }

    private BiomeGenBase[] fill(BiomeGenBase[] out, int n) {
        if (out == null || out.length < n) out = new BiomeGenBase[n];
        Arrays.fill(out, 0, n, biome);
        return out;
    }

    @Override
    public float[] getRainfall(float[] out, int x, int z, int w, int h) {
        if (out == null || out.length < w * h) out = new float[w * h];
        Arrays.fill(out, 0, w * h, rainfall);
        return out;
    }

    @Override
    public boolean areBiomesViable(int x, int z, int radius, List<?> allowed) {
        return allowed.contains(biome);
    }

    @Override
    public List<BiomeGenBase> getBiomesToSpawnIn() {
        List<BiomeGenBase> list = new ArrayList<>();
        list.add(biome);
        return list;
    }

    @Override
    public legacyconnector.v1_7_10.net.minecraft.world.ChunkPosition findBiomePosition(int x, int z, int range, List biomes, Random random) {
        return biomes.contains(biome) ? new legacyconnector.v1_7_10.net.minecraft.world.ChunkPosition(x - range + random.nextInt(range * 2 + 1), 0,
                z - range + random.nextInt(range * 2 + 1)) : null;
    }
}
