package legacyconnector.v1_7_10.net.minecraft.world.gen;

import java.util.Random;
import legacyconnector.v1_7_10.net.minecraft.block.Block;
import legacyconnector.v1_7_10.net.minecraft.init.Blocks;
import legacyconnector.v1_7_10.net.minecraft.world.World;
import legacyconnector.v1_7_10.net.minecraft.world.biome.BiomeGenBase;
import legacyconnector.v1_7_10.net.minecraft.world.chunk.Chunk;
import legacyconnector.v1_7_10.net.minecraft.world.chunk.IChunkProvider;

/**
 * Vanilla's overworld terrain generator: the 5x33x5 density noise interpolated to 4x8x4 cells, the biome blend, the
 * surface pass and caves. Dimensions of mods that build "an overworld" instantiate it directly.
 */
public class ChunkProviderGenerate implements IChunkProvider {
    private final Random rand;
    private final NoiseGeneratorOctaves noiseGen1;
    private final NoiseGeneratorOctaves noiseGen2;
    private final NoiseGeneratorOctaves noiseGen3;
    private final NoiseGeneratorPerlin surfaceNoise;
    public NoiseGeneratorOctaves noiseGen5;
    public NoiseGeneratorOctaves noiseGen6;
    public NoiseGeneratorOctaves mobSpawnerNoise;
    private final World worldObj;
    private final boolean mapFeaturesEnabled;
    private final double[] noiseField = new double[825];
    private final float[] parabolicField = new float[25];
    private double[] stoneNoise = new double[256];
    private double[] noise3;
    private double[] noise1;
    private double[] noise2;
    private double[] noise6;
    private BiomeGenBase[] biomesForGeneration;
    private final MapGenBase caveGenerator = new MapGenCaves();

    public ChunkProviderGenerate(World world, long seed, boolean mapFeaturesEnabled) {
        this.worldObj = world;
        this.mapFeaturesEnabled = mapFeaturesEnabled;
        this.rand = new Random(seed);
        this.noiseGen1 = new NoiseGeneratorOctaves(rand, 16);
        this.noiseGen2 = new NoiseGeneratorOctaves(rand, 16);
        this.noiseGen3 = new NoiseGeneratorOctaves(rand, 8);
        this.surfaceNoise = new NoiseGeneratorPerlin(rand, 4);
        this.noiseGen5 = new NoiseGeneratorOctaves(rand, 10);
        this.noiseGen6 = new NoiseGeneratorOctaves(rand, 16);
        this.mobSpawnerNoise = new NoiseGeneratorOctaves(rand, 8);
        for (int j = -2; j <= 2; j++) {
            for (int k = -2; k <= 2; k++) parabolicField[j + 2 + (k + 2) * 5] = 10.0F / (float) Math.sqrt(j * j + k * k + 0.2F);
        }
    }

    @Override
    public Chunk provideChunk(int cx, int cz) {
        rand.setSeed(cx * 341873128712L + cz * 132897987541L);
        Block[] blocks = new Block[65536];
        byte[] meta = new byte[65536];
        func_147424_a(cx, cz, blocks);
        biomesForGeneration = worldObj.getWorldChunkManager().loadBlockGeneratorData(biomesForGeneration, cx * 16, cz * 16, 16, 16);
        replaceBlocksForBiome(cx, cz, blocks, meta, biomesForGeneration);
        caveGenerator.generate(this, worldObj, cx, cz, blocks);
        Chunk chunk = new Chunk(worldObj, blocks, meta, cx, cz);
        byte[] biomes = new byte[256];
        for (int i = 0; i < 256; i++) biomes[i] = (byte) biomesForGeneration[i].biomeID;
        chunk.setBiomeArray(biomes);
        chunk.generateSkylightMap();
        return chunk;
    }

    public void func_147424_a(int cx, int cz, Block[] blocks) {
        final int seaLevel = 63;
        biomesForGeneration = worldObj.getWorldChunkManager().getBiomesForGeneration(biomesForGeneration, cx * 4 - 2, cz * 4 - 2, 10, 10);
        func_147423_a(cx * 4, 0, cz * 4);
        for (int cellX = 0; cellX < 4; cellX++) {
            int l = cellX * 5;
            int i1 = (cellX + 1) * 5;
            for (int cellZ = 0; cellZ < 4; cellZ++) {
                int j1 = (l + cellZ) * 33;
                int k1 = (l + cellZ + 1) * 33;
                int l1 = (i1 + cellZ) * 33;
                int i2 = (i1 + cellZ + 1) * 33;
                for (int cellY = 0; cellY < 32; cellY++) {
                    double d1 = noiseField[j1 + cellY];
                    double d2 = noiseField[k1 + cellY];
                    double d3 = noiseField[l1 + cellY];
                    double d4 = noiseField[i2 + cellY];
                    double d5 = (noiseField[j1 + cellY + 1] - d1) * 0.125D;
                    double d6 = (noiseField[k1 + cellY + 1] - d2) * 0.125D;
                    double d7 = (noiseField[l1 + cellY + 1] - d3) * 0.125D;
                    double d8 = (noiseField[i2 + cellY + 1] - d4) * 0.125D;
                    for (int dy = 0; dy < 8; dy++) {
                        double d10 = d1;
                        double d11 = d2;
                        double d12 = (d3 - d1) * 0.25D;
                        double d13 = (d4 - d2) * 0.25D;
                        int y = cellY * 8 + dy;
                        for (int dx = 0; dx < 4; dx++) {
                            double d15 = (d11 - d10) * 0.25D;
                            double d16 = d10 - d15;
                            int x = cellX * 4 + dx;
                            for (int dz = 0; dz < 4; dz++) {
                                int z = cellZ * 4 + dz;
                                int index = (x * 16 + z) * 256 + y;
                                if ((d16 += d15) > 0.0D) blocks[index] = Blocks.stone;
                                else if (y < seaLevel) blocks[index] = Blocks.water;
                            }
                            d10 += d12;
                            d11 += d13;
                        }
                        d1 += d5;
                        d2 += d6;
                        d3 += d7;
                        d4 += d8;
                    }
                }
            }
        }
    }

    public void replaceBlocksForBiome(int cx, int cz, Block[] blocks, byte[] meta, BiomeGenBase[] biomes) {
        double d0 = 0.03125D;
        stoneNoise = surfaceNoise.func_151599_a(stoneNoise, cx * 16, cz * 16, 16, 16, d0 * 2.0D, d0 * 2.0D, 1.0D);
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                BiomeGenBase biome = biomes[z + x * 16];
                biome.genTerrainBlocks(worldObj, rand, blocks, meta, cx * 16 + x, cz * 16 + z, stoneNoise[z + x * 16]);
            }
        }
    }

    public void func_147423_a(int x, int y, int z) {
        noise6 = noiseGen6.generateNoiseOctaves(noise6, x, z, 5, 5, 200.0D, 200.0D, 0.5D);
        double d0 = 684.412D;
        double d1 = 684.412D;
        noise3 = noiseGen3.generateNoiseOctaves(noise3, x, y, z, 5, 33, 5, d0 / 80.0D, d1 / 160.0D, d0 / 80.0D);
        noise1 = noiseGen1.generateNoiseOctaves(noise1, x, y, z, 5, 33, 5, d0, d1, d0);
        noise2 = noiseGen2.generateNoiseOctaves(noise2, x, y, z, 5, 33, 5, d0, d1, d0);
        int l = 0;
        int i1 = 0;
        for (int j1 = 0; j1 < 5; j1++) {
            for (int k1 = 0; k1 < 5; k1++) {
                float variation = 0.0F;
                float root = 0.0F;
                float weights = 0.0F;
                BiomeGenBase center = biomesForGeneration[j1 + 2 + (k1 + 2) * 10];
                for (int l1 = -2; l1 <= 2; l1++) {
                    for (int i2 = -2; i2 <= 2; i2++) {
                        BiomeGenBase b = biomesForGeneration[j1 + l1 + 2 + (k1 + i2 + 2) * 10];
                        float f3 = b.rootHeight;
                        float f4 = b.heightVariation;
                        float f5 = parabolicField[l1 + 2 + (i2 + 2) * 5] / (f3 + 2.0F);
                        if (b.rootHeight > center.rootHeight) f5 /= 2.0F;
                        variation += f4 * f5;
                        root += f3 * f5;
                        weights += f5;
                    }
                }
                variation /= weights;
                root /= weights;
                variation = variation * 0.9F + 0.1F;
                root = (root * 4.0F - 1.0F) / 8.0F;
                double d12 = noise6[i1] / 8000.0D;
                if (d12 < 0.0D) d12 = -d12 * 0.3D;
                d12 = d12 * 3.0D - 2.0D;
                if (d12 < 0.0D) {
                    d12 /= 2.0D;
                    if (d12 < -1.0D) d12 = -1.0D;
                    d12 /= 1.4D;
                    d12 /= 2.0D;
                } else {
                    if (d12 > 1.0D) d12 = 1.0D;
                    d12 /= 8.0D;
                }
                i1++;
                double d13 = root;
                double d14 = variation;
                d13 += d12 * 0.2D;
                d13 = d13 * 8.5D / 8.0D;
                double d5 = 8.5D + d13 * 4.0D;
                for (int j2 = 0; j2 < 33; j2++) {
                    double d6 = (j2 - d5) * 12.0D * 128.0D / 256.0D / d14;
                    if (d6 < 0.0D) d6 *= 4.0D;
                    double d7 = noise1[l] / 512.0D;
                    double d8 = noise2[l] / 512.0D;
                    double d9 = (noise3[l] / 10.0D + 1.0D) / 2.0D;
                    double d10 = (d9 < 0.0D ? d7 : d9 > 1.0D ? d8 : d7 + (d8 - d7) * d9) - d6;
                    if (j2 > 29) {
                        double d11 = (j2 - 29) / 3.0F;
                        d10 = d10 * (1.0D - d11) + -10.0D * d11;
                    }
                    noiseField[l] = d10;
                    l++;
                }
            }
        }
    }

    @Override
    public boolean chunkExists(int x, int z) {
        return true;
    }

    @Override
    public void populate(IChunkProvider provider, int cx, int cz) {
        int k = cx * 16;
        int l = cz * 16;
        BiomeGenBase biome = worldObj.getBiomeGenForCoords(k + 16, l + 16);
        rand.setSeed(worldObj.getSeed());
        long i1 = rand.nextLong() / 2L * 2L + 1L;
        long j1 = rand.nextLong() / 2L * 2L + 1L;
        rand.setSeed(cx * i1 + cz * j1 ^ worldObj.getSeed());
        biome.decorate(worldObj, rand, k, l);
        k += 8;
        l += 8;
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int top = worldObj.getPrecipitationHeight(k + x, l + z);
                if (top > 0 && top < 255 && biome.getFloatTemperature(k + x, top, l + z) < 0.15F && worldObj.getBlock(k + x, top, l + z) == Blocks.air
                        && worldObj.getBlock(k + x, top - 1, l + z) == Blocks.water) {
                    worldObj.setBlock(k + x, top - 1, l + z, Blocks.ice, 0, 2);
                }
            }
        }
    }

    @Override
    public String makeString() {
        return "RandomLevelSource";
    }

    @Override
    public int getLoadedChunkCount() {
        return 0;
    }
}
