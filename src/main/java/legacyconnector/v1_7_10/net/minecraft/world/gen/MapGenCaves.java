package legacyconnector.v1_7_10.net.minecraft.world.gen;

import java.util.Random;
import legacyconnector.v1_7_10.net.minecraft.block.Block;
import legacyconnector.v1_7_10.net.minecraft.init.Blocks;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/** Vanilla cave carver: worm tunnels and big chambers started from the chunks around the one being generated. */
public class MapGenCaves extends MapGenBase {
    @Override
    protected void func_151538_a(World world, int cx, int cz, int chunkX, int chunkZ, Block[] blocks) {
        int count = rand.nextInt(rand.nextInt(rand.nextInt(15) + 1) + 1);
        if (rand.nextInt(7) != 0) count = 0;
        for (int i = 0; i < count; i++) {
            double x = cx * 16 + rand.nextInt(16);
            double y = rand.nextInt(rand.nextInt(120) + 8);
            double z = cz * 16 + rand.nextInt(16);
            int tunnels = 1;
            if (rand.nextInt(4) == 0) {
                carve(rand.nextLong(), chunkX, chunkZ, blocks, x, y, z, 1.0F + rand.nextFloat() * 6.0F, 0.0F, 0.0F, -1, -1, 0.5D);
                tunnels += rand.nextInt(4);
            }
            for (int t = 0; t < tunnels; t++) {
                float yaw = rand.nextFloat() * (float) Math.PI * 2.0F;
                float pitch = (rand.nextFloat() - 0.5F) * 2.0F / 8.0F;
                float width = rand.nextFloat() * 2.0F + rand.nextFloat();
                if (rand.nextInt(10) == 0) width *= rand.nextFloat() * rand.nextFloat() * 3.0F + 1.0F;
                carve(rand.nextLong(), chunkX, chunkZ, blocks, x, y, z, width, yaw, pitch, 0, 0, 1.0D);
            }
        }
    }

    protected void carve(long seed, int chunkX, int chunkZ, Block[] blocks, double x, double y, double z, float width, float yaw, float pitch,
            int step, int maxStep, double scale) {
        double centerX = chunkX * 16 + 8;
        double centerZ = chunkZ * 16 + 8;
        float yawDrift = 0.0F;
        float pitchDrift = 0.0F;
        Random random = new Random(seed);
        if (maxStep <= 0) {
            int limit = range * 16 - 16;
            maxStep = limit - random.nextInt(limit / 4);
        }
        boolean chamber = false;
        if (step == -1) {
            step = maxStep / 2;
            chamber = true;
        }
        int branchAt = random.nextInt(maxStep / 2) + maxStep / 4;
        int height = blocks.length / 256;
        for (boolean steep = random.nextInt(6) == 0; step < maxStep; step++) {
            double radius = 1.5D + Math.sin(step * (float) Math.PI / maxStep) * width;
            double radiusY = radius * scale;
            float cosPitch = (float) Math.cos(pitch);
            x += Math.cos(yaw) * cosPitch;
            y += Math.sin(pitch);
            z += Math.sin(yaw) * cosPitch;
            pitch *= steep ? 0.92F : 0.7F;
            pitch += pitchDrift * 0.1F;
            yaw += yawDrift * 0.1F;
            pitchDrift *= 0.9F;
            yawDrift *= 0.75F;
            pitchDrift += (random.nextFloat() - random.nextFloat()) * random.nextFloat() * 2.0F;
            yawDrift += (random.nextFloat() - random.nextFloat()) * random.nextFloat() * 4.0F;
            if (!chamber && step == branchAt && width > 1.0F && maxStep > 0) {
                carve(random.nextLong(), chunkX, chunkZ, blocks, x, y, z, random.nextFloat() * 0.5F + 0.5F, yaw - (float) Math.PI / 2.0F, pitch / 3.0F, step, maxStep, 1.0D);
                carve(random.nextLong(), chunkX, chunkZ, blocks, x, y, z, random.nextFloat() * 0.5F + 0.5F, yaw + (float) Math.PI / 2.0F, pitch / 3.0F, step, maxStep, 1.0D);
                return;
            }
            if (!chamber && random.nextInt(4) == 0) continue;
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
            if (underWater(blocks, height, minX, maxX, minY, maxY, minZ, maxZ)) continue;
            for (int bx = minX; bx < maxX; bx++) {
                double rx = (bx + chunkX * 16 + 0.5D - x) / radius;
                for (int bz = minZ; bz < maxZ; bz++) {
                    double rz = (bz + chunkZ * 16 + 0.5D - z) / radius;
                    if (rx * rx + rz * rz >= 1.0D) continue;
                    boolean reachedGrass = false;
                    for (int by = maxY - 1; by >= minY; by--) {
                        double ry = (by + 0.5D - y) / radiusY;
                        if (ry <= -0.7D || rx * rx + ry * ry + rz * rz >= 1.0D) continue;
                        int i = (bx * 16 + bz) * height + by;
                        Block b = blocks[i];
                        if (b == Blocks.grass) reachedGrass = true;
                        if (b == Blocks.stone || b == Blocks.dirt || b == Blocks.grass) {
                            if (by < 10) {
                                blocks[i] = Blocks.lava;
                            } else {
                                blocks[i] = null;
                                if (reachedGrass && blocks[i - 1] == Blocks.dirt) blocks[i - 1] = Blocks.grass;
                            }
                        }
                    }
                }
            }
            if (chamber) break;
        }
    }

    private static boolean underWater(Block[] blocks, int height, int minX, int maxX, int minY, int maxY, int minZ, int maxZ) {
        for (int bx = minX; bx < maxX; bx++) {
            for (int bz = minZ; bz < maxZ; bz++) {
                for (int by = maxY + 1; by >= minY - 1; by--) {
                    if (by < 0 || by >= height) continue;
                    Block b = blocks[(bx * 16 + bz) * height + by];
                    if (b == Blocks.flowing_water || b == Blocks.water) return true;
                    if (by != minY - 1 && bx != minX && bx != maxX - 1 && bz != minZ && bz != maxZ - 1) by = minY;
                }
            }
        }
        return false;
    }

    private static int floor(double d) {
        int i = (int) d;
        return d < i ? i - 1 : i;
    }
}
