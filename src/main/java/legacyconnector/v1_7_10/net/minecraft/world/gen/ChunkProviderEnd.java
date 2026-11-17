package legacyconnector.v1_7_10.net.minecraft.world.gen;

import java.util.List;
import java.util.Random;
import legacyconnector.v1_7_10.net.minecraft.block.Block;
import legacyconnector.v1_7_10.net.minecraft.entity.EnumCreatureType;
import legacyconnector.v1_7_10.net.minecraft.init.Blocks;
import legacyconnector.v1_7_10.net.minecraft.util.MathHelper;
import legacyconnector.v1_7_10.net.minecraft.world.World;
import legacyconnector.v1_7_10.net.minecraft.world.biome.BiomeGenBase;
import legacyconnector.v1_7_10.net.minecraft.world.chunk.Chunk;
import legacyconnector.v1_7_10.net.minecraft.world.chunk.IChunkProvider;

/**
 * Vanilla's End generator: one island of end stone shaped by 3x33x3 density noise that falls off with the distance from the origin
 * and is cut at the top and bottom; populated through the biome's decorator. Dimensions of mods that extend {@code WorldProviderEnd}
 * without their own generator get this terrain.
 */
public class ChunkProviderEnd implements IChunkProvider {
    private final Random endRNG;
    private final NoiseGeneratorOctaves noiseGen1;
    private final NoiseGeneratorOctaves noiseGen2;
    private final NoiseGeneratorOctaves noiseGen3;
    public NoiseGeneratorOctaves noiseGen4;
    public NoiseGeneratorOctaves noiseGen5;
    private final World endWorld;
    private double[] densities;
    private double[] noiseData1;
    private double[] noiseData2;
    private double[] noiseData3;
    private double[] noiseData4;
    private double[] noiseData5;

    public ChunkProviderEnd(World world, long seed) {
        this.endWorld = world;
        this.endRNG = new Random(seed);
        this.noiseGen1 = new NoiseGeneratorOctaves(endRNG, 16);
        this.noiseGen2 = new NoiseGeneratorOctaves(endRNG, 16);
        this.noiseGen3 = new NoiseGeneratorOctaves(endRNG, 8);
        this.noiseGen4 = new NoiseGeneratorOctaves(endRNG, 10);
        this.noiseGen5 = new NoiseGeneratorOctaves(endRNG, 16);
    }

    /** Fills the blocks array (x, z, y with 128 heights) with end stone where the density is positive. */
    public void func_147420_a(int cx, int cz, Block[] blocks, BiomeGenBase[] biomes) {
        final int cells = 2;
        final int sx = cells + 1;
        final int sy = 33;
        final int sz = cells + 1;
        densities = initializeNoiseField(densities, cx * cells, 0, cz * cells, sx, sy, sz);
        for (int i = 0; i < cells; i++) {
            for (int j = 0; j < cells; j++) {
                for (int k = 0; k < 32; k++) {
                    double d1 = densities[((i + 0) * sz + j + 0) * sy + k + 0];
                    double d2 = densities[((i + 0) * sz + j + 1) * sy + k + 0];
                    double d3 = densities[((i + 1) * sz + j + 0) * sy + k + 0];
                    double d4 = densities[((i + 1) * sz + j + 1) * sy + k + 0];
                    double d5 = (densities[((i + 0) * sz + j + 0) * sy + k + 1] - d1) * 0.25D;
                    double d6 = (densities[((i + 0) * sz + j + 1) * sy + k + 1] - d2) * 0.25D;
                    double d7 = (densities[((i + 1) * sz + j + 0) * sy + k + 1] - d3) * 0.25D;
                    double d8 = (densities[((i + 1) * sz + j + 1) * sy + k + 1] - d4) * 0.25D;
                    for (int l = 0; l < 4; l++) {
                        double d10 = d1;
                        double d11 = d2;
                        double d12 = (d3 - d1) * 0.125D;
                        double d13 = (d4 - d2) * 0.125D;
                        for (int x = 0; x < 8; x++) {
                            int index = (x + i * 8) << 11 | (j * 8) << 7 | k * 4 + l;
                            double d15 = d10;
                            double d16 = (d11 - d10) * 0.125D;
                            for (int z = 0; z < 8; z++) {
                                blocks[index] = d15 > 0.0D ? Blocks.end_stone : null;
                                index += 128;
                                d15 += d16;
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

    @Override
    public Chunk loadChunk(int cx, int cz) {
        return provideChunk(cx, cz);
    }

    @Override
    public Chunk provideChunk(int cx, int cz) {
        endRNG.setSeed(cx * 341873128712L + cz * 132897987541L);
        Block[] blocks = new Block[32768];
        BiomeGenBase[] biomes = endWorld.getWorldChunkManager().loadBlockGeneratorData(null, cx * 16, cz * 16, 16, 16);
        func_147420_a(cx, cz, blocks, biomes);
        Chunk chunk = new Chunk(endWorld, blocks, cx, cz);
        byte[] ids = chunk.getBiomeArray();
        for (int i = 0; i < ids.length && biomes != null && i < biomes.length; i++) ids[i] = (byte) (biomes[i] == null ? 9 : biomes[i].biomeID);
        chunk.resetRelightChecks();
        return chunk;
    }

    /** The density of the island: two blended octave noises, strong near the origin and cut off at the top and bottom of the column. */
    private double[] initializeNoiseField(double[] field, int x, int y, int z, int sx, int sy, int sz) {
        if (field == null) field = new double[sx * sy * sz];
        double scale = 684.412D;
        double vscale = 684.412D;
        noiseData4 = noiseGen4.generateNoiseOctaves(noiseData4, x, z, sx, sz, 1.121D, 1.121D, 0.5D);
        noiseData5 = noiseGen5.generateNoiseOctaves(noiseData5, x, z, sx, sz, 200.0D, 200.0D, 0.5D);
        scale *= 2.0D;
        noiseData1 = noiseGen3.generateNoiseOctaves(noiseData1, x, y, z, sx, sy, sz, scale / 80.0D, vscale / 160.0D, scale / 80.0D);
        noiseData2 = noiseGen1.generateNoiseOctaves(noiseData2, x, y, z, sx, sy, sz, scale, vscale, scale);
        noiseData3 = noiseGen2.generateNoiseOctaves(noiseData3, x, y, z, sx, sy, sz, scale, vscale, scale);
        int index = 0;
        int column = 0;
        for (int i = 0; i < sx; i++) {
            for (int k = 0; k < sz; k++) {
                double d2 = (noiseData4[column] + 256.0D) / 512.0D;
                float fx = x + i;
                float fz = z + k;
                float island = 100.0F - MathHelper.sqrt_float(fx * fx + fz * fz) * 8.0F;
                if (island > 80.0F) island = 80.0F;
                if (island < -100.0F) island = -100.0F;
                if (d2 < 0.0D) d2 = 0.0D;
                d2 += 0.5D;
                column++;
                double mid = sy / 2.0D;
                for (int j = 0; j < sy; j++) {
                    double low = noiseData2[index] / 512.0D;
                    double high = noiseData3[index] / 512.0D;
                    double blend = (noiseData1[index] / 10.0D + 1.0D) / 2.0D;
                    double d = blend < 0.0D ? low : blend > 1.0D ? high : low + (high - low) * blend;
                    d -= 8.0D;
                    d += island;
                    int top = 2;
                    if (j > sy / 2 - top) {
                        double t = (float) (j - (sy / 2 - top)) / 64.0F;
                        if (t < 0.0D) t = 0.0D;
                        if (t > 1.0D) t = 1.0D;
                        d = d * (1.0D - t) + -3000.0D * t;
                    }
                    int bottom = 8;
                    if (j < bottom) {
                        double t = (float) (bottom - j) / ((float) bottom - 1.0F);
                        d = d * (1.0D - t) + -30.0D * t;
                    }
                    field[index] = d;
                    index++;
                }
            }
        }
        return field;
    }

    @Override
    public boolean chunkExists(int x, int z) {
        return true;
    }

    @Override
    public void populate(IChunkProvider provider, int cx, int cz) {
        int x = cx * 16;
        int z = cz * 16;
        BiomeGenBase biome = endWorld.getBiomeGenForCoords(x + 16, z + 16);
        if (biome != null) biome.decorate(endWorld, endRNG, x, z);
    }

    public boolean saveChunks(boolean all, Object progress) {
        return true;
    }

    @Override
    public boolean unloadQueuedChunks() {
        return false;
    }

    @Override
    public boolean canSave() {
        return true;
    }

    @Override
    public String makeString() {
        return "RandomLevelSource";
    }

    @Override
    public List getPossibleCreatures(EnumCreatureType type, int x, int y, int z) {
        BiomeGenBase biome = endWorld.getBiomeGenForCoords(x, z);
        return biome == null ? java.util.List.of() : biome.getSpawnableList(type);
    }

    @Override
    public int getLoadedChunkCount() {
        return 0;
    }

    @Override
    public void recreateStructures(int cx, int cz) {
    }

    @Override
    public void saveExtraData() {
    }
}
