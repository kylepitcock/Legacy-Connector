package legacyconnector.v1_12_2.net.minecraft.world.biome;

import java.util.List;
import java.util.Random;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;

/** 1.12.2 biome provider of a single biome everywhere (mod dimensions, the End, flat worlds). */
public class BiomeProviderSingle extends BiomeProvider {
    private final Biome biome;

    public BiomeProviderSingle(Biome biome) {
        this.biome = biome;
    }

    @Override
    public Biome getBiome(BlockPos pos) {
        return biome;
    }

    @Override
    public Biome getBiome(BlockPos pos, Biome defaultBiome) {
        return biome;
    }

    @Override
    public Biome[] getBiomesForGeneration(Biome[] biomes, int x, int z, int width, int height) {
        if (biomes == null || biomes.length < width * height) biomes = new Biome[width * height];
        java.util.Arrays.fill(biomes, 0, width * height, biome);
        return biomes;
    }

    @Override
    public Biome[] getBiomes(Biome[] oldBiomeList, int x, int z, int width, int depth) {
        return getBiomesForGeneration(oldBiomeList, x, z, width, depth);
    }

    @Override
    public Biome[] getBiomes(Biome[] listToReuse, int x, int z, int width, int length, boolean cacheFlag) {
        return getBiomesForGeneration(listToReuse, x, z, width, length);
    }

    @Override
    public BlockPos findBiomePosition(int x, int z, int range, List<Biome> biomes, Random random) {
        return biomes.contains(biome) ? new BlockPos(x - range + random.nextInt(range * 2 + 1), 0, z - range + random.nextInt(range * 2 + 1)) : null;
    }

    @Override
    public boolean areBiomesViable(int x, int z, int radius, List<Biome> allowed) {
        return allowed.contains(biome);
    }

    @Override
    public boolean isFixedBiome() {
        return true;
    }

    @Override
    public Biome getFixedBiome() {
        return biome;
    }
}
