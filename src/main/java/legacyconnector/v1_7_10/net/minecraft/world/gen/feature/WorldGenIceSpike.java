package legacyconnector.v1_7_10.net.minecraft.world.gen.feature;

import java.util.Random;
import legacyconnector.v1_7_10.net.minecraft.block.Block;
import legacyconnector.v1_7_10.net.minecraft.init.Blocks;
import legacyconnector.v1_7_10.net.minecraft.util.MathHelper;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/** A tall tapering spike of packed ice rooted in the snow (the ice plains spikes biome). */
public class WorldGenIceSpike extends WorldGenerator {
    private static boolean replaceable(World world, Block block, int x, int y, int z) {
        return world.isAirBlock(x, y, z) || block == Blocks.dirt || block == Blocks.snow || block == Blocks.ice;
    }

    @Override
    public boolean generate(World world, Random random, int x, int y, int z) {
        while (world.isAirBlock(x, y, z) && y > 2) --y;
        if (world.getBlock(x, y, z) != Blocks.snow) return false;
        y += random.nextInt(4);
        int height = random.nextInt(4) + 7;
        int width = height / 4 + random.nextInt(2);
        if (width > 1 && random.nextInt(60) == 0) y += 10 + random.nextInt(30);
        for (int h = 0; h < height; h++) {
            float f = (1.0F - (float) h / (float) height) * (float) width;
            int r = MathHelper.ceiling_float_int(f);
            for (int dx = -r; dx <= r; dx++) {
                float f1 = (float) MathHelper.abs_int(dx) - 0.25F;
                for (int dz = -r; dz <= r; dz++) {
                    float f2 = (float) MathHelper.abs_int(dz) - 0.25F;
                    if ((dx == 0 && dz == 0 || f1 * f1 + f2 * f2 <= f * f) && (dx != -r && dx != r && dz != -r && dz != r || random.nextFloat() <= 0.75F)) {
                        if (replaceable(world, world.getBlock(x + dx, y + h, z + dz), x + dx, y + h, z + dz)) {
                            world.setBlock(x + dx, y + h, z + dz, Blocks.packed_ice, 0, 2);
                        }
                        if (h != 0 && r > 1 && replaceable(world, world.getBlock(x + dx, y - h, z + dz), x + dx, y - h, z + dz)) {
                            world.setBlock(x + dx, y - h, z + dz, Blocks.packed_ice, 0, 2);
                        }
                    }
                }
            }
        }
        int spread = Math.max(0, Math.min(1, width - 1));
        for (int dx = -spread; dx <= spread; dx++) {
            for (int dz = -spread; dz <= spread; dz++) {
                int py = y - 1;
                int run = 50;
                if (MathHelper.abs_int(dx) == 1 && MathHelper.abs_int(dz) == 1) run = random.nextInt(5);
                while (py > 50) {
                    Block block = world.getBlock(x + dx, py, z + dz);
                    if (!(replaceable(world, block, x + dx, py, z + dz) || block == Blocks.packed_ice)) break;
                    world.setBlock(x + dx, py, z + dz, Blocks.packed_ice, 0, 2);
                    py--;
                    run--;
                    if (run <= 0) {
                        py -= random.nextInt(5) + 1;
                        run = random.nextInt(5);
                    }
                }
            }
        }
        return true;
    }
}
