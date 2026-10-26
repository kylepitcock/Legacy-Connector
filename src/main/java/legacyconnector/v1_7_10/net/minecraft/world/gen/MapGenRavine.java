package legacyconnector.v1_7_10.net.minecraft.world.gen;

import java.util.Random;
import legacyconnector.v1_7_10.net.minecraft.block.Block;
import legacyconnector.v1_7_10.net.minecraft.init.Blocks;
import legacyconnector.v1_7_10.net.minecraft.world.World;
import legacyconnector.v1_7_10.net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.util.Mth;

/**
 * Vanilla's ravine carver: one ravine in fifty chunks, a tall narrow worm with lava at its bottom, kept away from water. Legacy chunk
 * providers (mod dimensions) run it next to the cave carver; the overworld's ravines are the modern game's.
 */
public class MapGenRavine extends MapGenBase {
    private final float[] field_75046_d = new float[1024];

    /** One ravine: the worm carved where it passes through this chunk's column arrays. */
    protected void func_151540_a(long seed, int chunkX, int chunkZ, Block[] blocks, double x, double y, double z, float width, float yaw, float pitch,
            int step, int maxStep, double heightRatio) {
        if (blocks == null) return;
        int height = Math.max(1, blocks.length / 256);
        Random random = new Random(seed);
        double centerX = chunkX * 16 + 8;
        double centerZ = chunkZ * 16 + 8;
        float yawDrift = 0.0F;
        float pitchDrift = 0.0F;
        if (maxStep <= 0) {
            int range = this.range * 16 - 16;
            maxStep = range - random.nextInt(range / 4);
        }
        boolean single = false;
        if (step == -1) {
            step = maxStep / 2;
            single = true;
        }
        float wall = 1.0F;
        for (int i = 0; i < 256; i++) {
            if (i == 0 || random.nextInt(3) == 0) wall = 1.0F + random.nextFloat() * random.nextFloat() * 1.0F;
            field_75046_d[i] = wall * wall;
        }
        for (; step < maxStep; step++) {
            double radius = 1.5D + Mth.sin(step * (float) Math.PI / maxStep) * width * 1.0F;
            double radiusY = radius * heightRatio;
            radius *= random.nextFloat() * 0.25D + 0.75D;
            radiusY *= random.nextFloat() * 0.25D + 0.75D;
            float cosPitch = Mth.cos(pitch);
            float sinPitch = Mth.sin(pitch);
            x += Mth.cos(yaw) * cosPitch;
            y += sinPitch;
            z += Mth.sin(yaw) * cosPitch;
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
            double left = maxStep - step;
            double reach = width + 2.0F + 16.0F;
            if (dx * dx + dz * dz - left * left > reach * reach) return;
            if (x < centerX - 16.0D - radius * 2.0D || z < centerZ - 16.0D - radius * 2.0D || x > centerX + 16.0D + radius * 2.0D
                    || z > centerZ + 16.0D + radius * 2.0D) {
                continue;
            }
            int x1 = Math.max(0, Mth.floor(x - radius) - chunkX * 16 - 1);
            int x2 = Math.min(16, Mth.floor(x + radius) - chunkX * 16 + 1);
            int y1 = Math.max(1, Mth.floor(y - radiusY) - 1);
            int y2 = Math.min(height - 8, Mth.floor(y + radiusY) + 1);
            int z1 = Math.max(0, Mth.floor(z - radius) - chunkZ * 16 - 1);
            int z2 = Math.min(16, Mth.floor(z + radius) - chunkZ * 16 + 1);
            boolean water = false;
            for (int bx = x1; !water && bx < x2; bx++) {
                for (int bz = z1; !water && bz < z2; bz++) {
                    for (int by = y2 + 1; !water && by >= y1 - 1; by--) {
                        if (by < 0 || by >= height) continue;
                        int i = (bx * 16 + bz) * height + by;
                        if (isOceanBlock(blocks, i, bx, by, bz, chunkX, chunkZ)) water = true;
                        if (by != y1 - 1 && bx != x1 && bx != x2 - 1 && bz != z1 && bz != z2 - 1) by = y1;
                    }
                }
            }
            if (water) continue;
            for (int bx = x1; bx < x2; bx++) {
                double ex = (bx + chunkX * 16 + 0.5D - x) / radius;
                for (int bz = z1; bz < z2; bz++) {
                    double ez = (bz + chunkZ * 16 + 0.5D - z) / radius;
                    int i = (bx * 16 + bz) * height + y2;
                    boolean foundTop = false;
                    if (ex * ex + ez * ez < 1.0D) {
                        for (int by = y2 - 1; by >= y1; by--) {
                            double ey = (by + 0.5D - y) / radiusY;
                            if ((ex * ex + ez * ez) * field_75046_d[Math.min(by, field_75046_d.length - 1)] + ey * ey / 6.0D < 1.0D && i >= 0 && i < blocks.length) {
                                if (isTopBlock(blocks, i, bx, by, bz, chunkX, chunkZ)) foundTop = true;
                                digBlock(blocks, i, bx, by, bz, chunkX, chunkZ, foundTop);
                            }
                            i--;
                        }
                    }
                }
            }
            if (single) break;
        }
    }

    @Override
    protected void func_151538_a(World world, int x, int z, int chunkX, int chunkZ, Block[] blocks) {
        if (blocks == null || rand.nextInt(50) != 0) return;
        double rx = x * 16 + rand.nextInt(16);
        double ry = rand.nextInt(rand.nextInt(40) + 8) + 20;
        double rz = z * 16 + rand.nextInt(16);
        float yaw = rand.nextFloat() * (float) Math.PI * 2.0F;
        float pitch = (rand.nextFloat() - 0.5F) * 2.0F / 8.0F;
        float width = (rand.nextFloat() * 2.0F + rand.nextFloat()) * 2.0F;
        func_151540_a(rand.nextLong(), chunkX, chunkZ, blocks, rx, ry, rz, width, yaw, pitch, 0, 0, 3.0D);
    }

    /** Forge: whether the block stops the ravine (water: ravines do not cut into oceans and lakes). */
    protected boolean isOceanBlock(Block[] data, int index, int x, int y, int z, int chunkX, int chunkZ) {
        return data[index] == Blocks.flowing_water || data[index] == Blocks.water;
    }

    private static boolean isExceptionBiome(BiomeGenBase biome) {
        return biome == BiomeGenBase.mushroomIsland || biome == BiomeGenBase.beach || biome == BiomeGenBase.desert;
    }

    private BiomeGenBase biomeAt(int x, int z, int chunkX, int chunkZ) {
        BiomeGenBase biome = worldObj == null ? null : worldObj.getBiomeGenForCoords(x + chunkX * 16, z + chunkZ * 16);
        return biome == null ? BiomeGenBase.plains : biome;
    }

    /** Forge: whether the block is the biome's surface block (the ravine then re-grasses the dirt it opens). */
    private boolean isTopBlock(Block[] data, int index, int x, int y, int z, int chunkX, int chunkZ) {
        BiomeGenBase biome = biomeAt(x, z, chunkX, chunkZ);
        return isExceptionBiome(biome) ? data[index] == Blocks.grass : data[index] == biome.topBlock;
    }

    /** Forge: carves one block: stone and the biome's surface blocks become air (lava below y=10). */
    protected void digBlock(Block[] data, int index, int x, int y, int z, int chunkX, int chunkZ, boolean foundTop) {
        BiomeGenBase biome = biomeAt(x, z, chunkX, chunkZ);
        Block top = isExceptionBiome(biome) ? Blocks.grass : biome.topBlock;
        Block filler = isExceptionBiome(biome) ? Blocks.dirt : biome.fillerBlock;
        Block block = data[index];
        if (block != Blocks.stone && block != filler && block != top) return;
        if (y < 10) {
            data[index] = Blocks.flowing_lava;
        } else {
            data[index] = null;
            if (foundTop && index > 0 && data[index - 1] == filler) data[index - 1] = top;
        }
    }
}
