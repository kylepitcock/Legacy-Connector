package legacyconnector.v1_7_10.net.minecraft.world.gen.feature;

import java.util.Random;
import legacyconnector.v1_7_10.net.minecraft.block.Block;
import legacyconnector.v1_7_10.net.minecraft.init.Blocks;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/** A boulder of three overlapping balls of {@code block} resting on grass, dirt or stone (vanilla: mega taiga's mossy cobblestone). */
public class WorldGenBlockBlob extends WorldGenerator {
    public Block field_150545_a;
    public int field_150544_b;

    public WorldGenBlockBlob(Block block, int radius) {
        super(false);
        this.field_150545_a = block;
        this.field_150544_b = radius;
    }

    @Override
    public boolean generate(World world, Random random, int x, int y, int z) {
        while (y > 3) {
            if (!world.isAirBlock(x, y - 1, z)) {
                Block below = world.getBlock(x, y - 1, z);
                if (below == Blocks.grass || below == Blocks.dirt || below == Blocks.stone) break;
            }
            y--;
        }
        if (y <= 3) return false;
        int radius = field_150544_b;
        for (int ball = 0; radius >= 0 && ball < 3; ball++) {
            int rx = radius + random.nextInt(2);
            int ry = radius + random.nextInt(2);
            int rz = radius + random.nextInt(2);
            float r = (rx + ry + rz) * 0.333F + 0.5F;
            for (int bx = x - rx; bx <= x + rx; bx++) {
                for (int bz = z - rz; bz <= z + rz; bz++) {
                    for (int by = y - ry; by <= y + ry; by++) {
                        float dx = bx - x, dy = by - y, dz = bz - z;
                        if (dx * dx + dz * dz + dy * dy <= r * r) world.setBlock(bx, by, bz, field_150545_a, 0, 4);
                    }
                }
            }
            x += -(radius + 1) + random.nextInt(2 + radius * 2);
            z += -(radius + 1) + random.nextInt(2 + radius * 2);
            y -= random.nextInt(2);
        }
        return true;
    }
}
