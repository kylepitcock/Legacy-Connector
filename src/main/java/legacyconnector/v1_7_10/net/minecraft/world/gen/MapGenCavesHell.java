package legacyconnector.v1_7_10.net.minecraft.world.gen;

import java.util.Random;
import legacyconnector.v1_7_10.net.minecraft.block.Block;
import legacyconnector.v1_7_10.net.minecraft.init.Blocks;
import legacyconnector.v1_7_10.net.minecraft.world.World;
import net.minecraft.util.Mth;

/** The nether's cave carver: wide tunnels through netherrack in a 128-high chunk, stopping short of lava. */
public class MapGenCavesHell extends MapGenBase {
    /** A big chamber with tunnels out of it. */
    protected void func_151544_a(long seed, int chunkX, int chunkZ, Block[] blocks, double x, double y, double z) {
        func_151543_a(seed, chunkX, chunkZ, blocks, x, y, z, 1.0F + rand.nextFloat() * 6.0F, 0.0F, 0.0F, -1, -1, 0.5D);
    }

    /** One tunnel: a worm of ellipsoids carved where it passes through this chunk. */
    protected void func_151543_a(long seed, int chunkX, int chunkZ, Block[] blocks, double x, double y, double z, float width, float yaw, float pitch,
            int step, int maxStep, double heightRatio) {
        double centerX = chunkX * 16 + 8;
        double centerZ = chunkZ * 16 + 8;
        float yawDrift = 0.0F;
        float pitchDrift = 0.0F;
        Random random = new Random(seed);
        if (maxStep <= 0) {
            int range = this.range * 16 - 16;
            maxStep = range - random.nextInt(range / 4);
        }
        boolean room = false;
        if (step == -1) {
            step = maxStep / 2;
            room = true;
        }
        int branchAt = random.nextInt(maxStep / 2) + maxStep / 4;
        for (boolean steep = random.nextInt(6) == 0; step < maxStep; step++) {
            double radius = 1.5D + Mth.sin(step * (float) Math.PI / maxStep) * width * 1.0F;
            double radiusY = radius * heightRatio;
            float cosPitch = Mth.cos(pitch);
            float sinPitch = Mth.sin(pitch);
            x += Mth.cos(yaw) * cosPitch;
            y += sinPitch;
            z += Mth.sin(yaw) * cosPitch;
            pitch *= steep ? 0.92F : 0.7F;
            pitch += pitchDrift * 0.1F;
            yaw += yawDrift * 0.1F;
            pitchDrift *= 0.9F;
            yawDrift *= 0.75F;
            pitchDrift += (random.nextFloat() - random.nextFloat()) * random.nextFloat() * 2.0F;
            yawDrift += (random.nextFloat() - random.nextFloat()) * random.nextFloat() * 4.0F;
            if (!room && step == branchAt && width > 1.0F) {
                func_151543_a(random.nextLong(), chunkX, chunkZ, blocks, x, y, z, random.nextFloat() * 0.5F + 0.5F, yaw - (float) Math.PI / 2.0F, pitch / 3.0F,
                        step, maxStep, 1.0D);
                func_151543_a(random.nextLong(), chunkX, chunkZ, blocks, x, y, z, random.nextFloat() * 0.5F + 0.5F, yaw + (float) Math.PI / 2.0F, pitch / 3.0F,
                        step, maxStep, 1.0D);
                return;
            }
            if (!room && random.nextInt(4) == 0) continue;
            double dx = x - centerX;
            double dz = z - centerZ;
            double left = maxStep - step;
            double reach = width + 2.0F + 16.0F;
            if (dx * dx + dz * dz - left * left > reach * reach) return;
            if (x < centerX - 16.0D - radius * 2.0D || z < centerZ - 16.0D - radius * 2.0D || x > centerX + 16.0D + radius * 2.0D
                    || z > centerZ + 16.0D + radius * 2.0D) {
                continue;
            }
            int x1 = Mth.floor(x - radius) - chunkX * 16 - 1;
            int x2 = Mth.floor(x + radius) - chunkX * 16 + 1;
            int y1 = Mth.floor(y - radiusY) - 1;
            int y2 = Mth.floor(y + radiusY) + 1;
            int z1 = Mth.floor(z - radius) - chunkZ * 16 - 1;
            int z2 = Mth.floor(z + radius) - chunkZ * 16 + 1;
            if (x1 < 0) x1 = 0;
            if (x2 > 16) x2 = 16;
            if (y1 < 1) y1 = 1;
            if (y2 > 120) y2 = 120;
            if (z1 < 0) z1 = 0;
            if (z2 > 16) z2 = 16;
            boolean lava = false;
            for (int bx = x1; !lava && bx < x2; bx++) {
                for (int bz = z1; !lava && bz < z2; bz++) {
                    for (int by = y2 + 1; !lava && by >= y1 - 1; by--) {
                        int i = (bx * 16 + bz) * 128 + by;
                        if (by < 0 || by >= 128 || i < 0 || i >= blocks.length) continue;
                        Block b = blocks[i];
                        if (b == Blocks.flowing_lava || b == Blocks.lava) lava = true;
                        if (by != y1 - 1 && bx != x1 && bx != x2 - 1 && bz != z1 && bz != z2 - 1) by = y1;
                    }
                }
            }
            if (lava) continue;
            for (int bx = x1; bx < x2; bx++) {
                double ex = (bx + chunkX * 16 + 0.5D - x) / radius;
                for (int bz = z1; bz < z2; bz++) {
                    double ez = (bz + chunkZ * 16 + 0.5D - z) / radius;
                    int i = (bx * 16 + bz) * 128 + y2;
                    for (int by = y2 - 1; by >= y1; by--) {
                        double ey = (by + 0.5D - y) / radiusY;
                        if (ey > -0.7D && ex * ex + ey * ey + ez * ez < 1.0D && i >= 0 && i < blocks.length) {
                            Block b = blocks[i];
                            if (b == Blocks.netherrack || b == Blocks.dirt || b == Blocks.grass) blocks[i] = null;
                        }
                        i--;
                    }
                }
            }
            if (room) break;
        }
    }

    @Override
    protected void func_151538_a(World world, int cx, int cz, int chunkX, int chunkZ, Block[] blocks) {
        if (blocks == null) return;
        int count = rand.nextInt(rand.nextInt(rand.nextInt(10) + 1) + 1);
        if (rand.nextInt(5) != 0) count = 0;
        for (int i = 0; i < count; i++) {
            double x = cx * 16 + rand.nextInt(16);
            double y = rand.nextInt(128);
            double z = cz * 16 + rand.nextInt(16);
            int tunnels = 1;
            if (rand.nextInt(4) == 0) {
                func_151544_a(rand.nextLong(), chunkX, chunkZ, blocks, x, y, z);
                tunnels += rand.nextInt(4);
            }
            for (int t = 0; t < tunnels; t++) {
                float yaw = rand.nextFloat() * (float) Math.PI * 2.0F;
                float pitch = (rand.nextFloat() - 0.5F) * 2.0F / 8.0F;
                float width = rand.nextFloat() * 2.0F + rand.nextFloat();
                func_151543_a(rand.nextLong(), chunkX, chunkZ, blocks, x, y, z, width * 2.0F, yaw, pitch, 0, 0, 0.5D);
            }
        }
    }
}
