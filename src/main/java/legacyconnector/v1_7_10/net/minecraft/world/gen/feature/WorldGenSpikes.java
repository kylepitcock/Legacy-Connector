package legacyconnector.v1_7_10.net.minecraft.world.gen.feature;

import java.util.Random;
import legacyconnector.v1_7_10.net.minecraft.block.Block;
import legacyconnector.v1_7_10.net.minecraft.init.Blocks;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/** The obsidian pillars of the End; {@code generate} builds one pillar of random height and radius at the position. */
public class WorldGenSpikes extends WorldGenerator {
    private final Block field_150520_a;

    public WorldGenSpikes(Block block) {
        this.field_150520_a = block;
    }

    @Override
    public boolean generate(World world, Random random, int x, int y, int z) {
        if (!world.isAirBlock(x, y, z) || world.getBlock(x, y - 1, z) != field_150520_a) return false;
        int radius = random.nextInt(32) + 6;
        int height = random.nextInt(4) + 1;
        for (int dx = -height; dx <= height; dx++) {
            for (int dz = -height; dz <= height; dz++) {
                if (dx * dx + dz * dz > height * height + 1) continue;
                for (int dy = 0; dy < radius; dy++) {
                    if (world.getBlock(x + dx, y + dy, z + dz).getMaterial().isReplaceable() || world.isAirBlock(x + dx, y + dy, z + dz)) {
                        setBlockAndNotifyAdequately(world, x + dx, y + dy, z + dz, Blocks.obsidian, 0);
                    }
                }
            }
        }
        return true;
    }
}
