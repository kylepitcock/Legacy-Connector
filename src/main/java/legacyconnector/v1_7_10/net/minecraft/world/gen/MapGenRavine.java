package legacyconnector.v1_7_10.net.minecraft.world.gen;

import java.util.Random;
import legacyconnector.v1_7_10.net.minecraft.block.Block;
import legacyconnector.v1_7_10.net.minecraft.init.Blocks;
import legacyconnector.v1_7_10.net.minecraft.world.World;
import legacyconnector.v1_7_10.net.minecraft.world.biome.BiomeGenBase;

/**
 * Vanilla 1.7.10 ravine carver (with Forge's block hooks): one chunk in 50 starts a deep, narrow, wavy-walled cut. Mods
 * with their own terrain run it in their chunk providers, and extend it (their own walls and fluids through
 * {@link #isOceanBlock} and {@link #digBlock}, or their own ravine shapes through {@link #func_151540_a}). It works on the
 * legacy chunk block array only, so it never touches the world.
 */
public class MapGenRavine extends MapGenBase {
    /** Squared wall roughness per height. */
    private float[] field_75046_d = new float[1024];

    /**
     * Carves one ravine segment by segment from (x, y, z): {@code width} its half width, {@code yaw}/{@code pitch} its
     * direction, {@code step}/{@code maxStep} the segment range (0, 0: a whole ravine; -1: one chamber-like cut), {@code scaleY}
     * its depth relative to its width.
     */
    protected void func_151540_a(long seed, int chunkX, int chunkZ, Block[] blocks, double x, double y, double z, float width, float yaw, float pitch,
            int step, int maxStep, double scaleY) {
        Random random = new Random(seed);
        double centerX = chunkX * 16 + 8;
        double centerZ = chunkZ * 16 + 8;
        float yawDrift = 0.0F;
        float pitchDrift = 0.0F;
        if (maxStep <= 0) {
            int limit = range * 16 - 16;
            maxStep = limit - random.nextInt(limit / 4);
        }
        boolean single = false;
        if (step == -1) {
            step = maxStep / 2;
            single = true;
        }
        int height = blocks.length / 256;
        if (field_75046_d.length < height) field_75046_d = new float[height];
        float rough = 1.0F;
        for (int k = 0; k < Math.min(256, height); ++k) {
            if (k == 0 || random.nextInt(3) == 0) rough = 1.0F + random.nextFloat() * random.nextFloat();
            field_75046_d[k] = rough * rough;
        }
        for (; step < maxStep; ++step) {
            double radius = 1.5D + Math.sin(step * (float) Math.PI / maxStep) * width;
            double radiusY = radius * scaleY;
            radius *= random.nextFloat() * 0.25D + 0.75D;
            radiusY *= random.nextFloat() * 0.25D + 0.75D;
            float cosPitch = (float) Math.cos(pitch);
            float sinPitch = (float) Math.sin(pitch);
            x += Math.cos(yaw) * cosPitch;
            y += sinPitch;
            z += Math.sin(yaw) * cosPitch;
            pitch *= 0.7F;
            pitch += pitchDrift * 0.05F;
            yaw += yawDrift * 0.05F;
            pitchDrift *= 0.8F;
            yawDrift *= 0.5F;
            pitchDrift += (random.nextFloat() - random.nextFloat()) * random.nextFloat() * 2.0F;
            yawDrift += (random.nextFloat() - random.nextFloat()) * random.nextFloat() * 4.0F;
            if (!single && random.nextInt(4) == 0) continue;
            double dx = x - centerX;
            double dz = z - centerZ;
            double remaining = maxStep - step;
            double reach = width + 2.0F + 16.0F;
            if (dx * dx + dz * dz - remaining * remaining > reach * reach) return;
            if (x < centerX - 16.0D - radius * 2.0D || z < centerZ - 16.0D - radius * 2.0D || x > centerX + 16.0D + radius * 2.0D
                    || z > centerZ + 16.0D + radius * 2.0D) continue;
            int minX = Math.max(0, floor(x - radius) - chunkX * 16 - 1);
            int maxX = Math.min(16, floor(x + radius) - chunkX * 16 + 1);
            int minY = Math.max(1, floor(y - radiusY) - 1);
            int maxY = Math.min(height - 8, floor(y + radiusY) + 1);
            int minZ = Math.max(0, floor(z - radius) - chunkZ * 16 - 1);
            int maxZ = Math.min(16, floor(z + radius) - chunkZ * 16 + 1);
            boolean water = false;
            for (int bx = minX; !water && bx < maxX; ++bx) {
                for (int bz = minZ; !water && bz < maxZ; ++bz) {
                    for (int by = maxY + 1; !water && by >= minY - 1; --by) {
                        if (by < 0 || by >= height) continue;
                        int index = (bx * 16 + bz) * height + by;
                        if (isOceanBlock(blocks, index, bx, by, bz, chunkX, chunkZ)) water = true;
                        if (by != minY - 1 && bx != minX && bx != maxX - 1 && bz != minZ && bz != maxZ - 1) by = minY;
                    }
                }
            }
            if (water) continue;
            for (int bx = minX; bx < maxX; ++bx) {
                double rx = (bx + chunkX * 16 + 0.5D - x) / radius;
                for (int bz = minZ; bz < maxZ; ++bz) {
                    double rz = (bz + chunkZ * 16 + 0.5D - z) / radius;
                    if (rx * rx + rz * rz >= 1.0D) continue;
                    // as in 1.7.10, the index runs one block above the height it is checked for
                    int index = (bx * 16 + bz) * height + maxY;
                    boolean foundTop = false;
                    for (int by = maxY - 1; by >= minY; --by) {
                        double ry = (by + 0.5D - y) / radiusY;
                        if ((rx * rx + rz * rz) * field_75046_d[by] + ry * ry / 6.0D < 1.0D) {
                            if (isTopBlock(blocks, index, bx, by, bz, chunkX, chunkZ)) foundTop = true;
                            digBlock(blocks, index, bx, by, bz, chunkX, chunkZ, foundTop);
                        }
                        --index;
                    }
                }
            }
            if (single) break;
        }
    }

    @Override
    protected void func_151538_a(World world, int cx, int cz, int chunkX, int chunkZ, Block[] blocks) {
        if (rand.nextInt(50) != 0) return;
        double x = cx * 16 + rand.nextInt(16);
        double y = rand.nextInt(rand.nextInt(40) + 8) + 20;
        double z = cz * 16 + rand.nextInt(16);
        float yaw = rand.nextFloat() * (float) Math.PI * 2.0F;
        float pitch = (rand.nextFloat() - 0.5F) * 2.0F / 8.0F;
        float width = (rand.nextFloat() * 2.0F + rand.nextFloat()) * 2.0F;
        func_151540_a(rand.nextLong(), chunkX, chunkZ, blocks, x, y, z, width, yaw, pitch, 0, 0, 3.0D);
    }

    /** Forge: whether the block stops the ravine (water: ravines do not cut into oceans and lakes). */
    protected boolean isOceanBlock(Block[] data, int index, int x, int y, int z, int chunkX, int chunkZ) {
        return data[index] == Blocks.flowing_water || data[index] == Blocks.water;
    }

    private static boolean isExceptionBiome(BiomeGenBase biome) {
        return biome == BiomeGenBase.mushroomIsland || biome == BiomeGenBase.beach || biome == BiomeGenBase.desert;
    }

    private int mcc$biomeX = Integer.MIN_VALUE, mcc$biomeZ;
    private BiomeGenBase mcc$biome;

    /** The biome of a column (the last one is kept: a ravine asks once per carved block). Plains when the world has none. */
    private BiomeGenBase mcc$biome(int x, int z) {
        if (x != mcc$biomeX || z != mcc$biomeZ || mcc$biome == null) {
            BiomeGenBase biome = null;
            try {
                if (worldObj != null) biome = worldObj.getBiomeGenForCoords(x, z);
            } catch (RuntimeException e) {
                biome = null;
            }
            mcc$biome = biome != null ? biome : BiomeGenBase.plains;
            mcc$biomeX = x;
            mcc$biomeZ = z;
        }
        return mcc$biome;
    }

    private boolean isTopBlock(Block[] data, int index, int x, int y, int z, int chunkX, int chunkZ) {
        BiomeGenBase biome = mcc$biome(x + chunkX * 16, z + chunkZ * 16);
        return isExceptionBiome(biome) ? data[index] == Blocks.grass : data[index] == biome.topBlock;
    }

    /** Forge: removes one block (lava below y 10), turning the filler below a removed top block into top block. */
    protected void digBlock(Block[] data, int index, int x, int y, int z, int chunkX, int chunkZ, boolean foundTop) {
        BiomeGenBase biome = mcc$biome(x + chunkX * 16, z + chunkZ * 16);
        Block top = isExceptionBiome(biome) ? Blocks.grass : biome.topBlock;
        Block filler = isExceptionBiome(biome) ? Blocks.dirt : biome.fillerBlock;
        Block block = data[index];
        if (block == Blocks.stone || block == filler || block == top) {
            if (y < 10) {
                data[index] = Blocks.flowing_lava;
            } else {
                data[index] = null;
                if (foundTop && index > 0 && data[index - 1] == filler) data[index - 1] = top;
            }
        }
    }

    private static int floor(double d) {
        int i = (int) d;
        return d < i ? i - 1 : i;
    }
}
