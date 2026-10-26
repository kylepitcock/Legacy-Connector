package legacyconnector.v1_7_10.net.minecraft.world.gen.feature;

import java.util.Random;
import legacyconnector.v1_7_10.net.minecraft.init.Blocks;
import legacyconnector.v1_7_10.net.minecraft.world.World;
import net.minecraft.util.Mth;

/** The 2x2 jungle tree: a vine-covered trunk with side branches ending in leaf clumps and a crown of leaf discs. */
public class WorldGenMegaJungle extends WorldGenHugeTrees {
    public WorldGenMegaJungle(boolean notify, int baseHeight, int extraHeight, int woodMeta, int leavesMeta) {
        super(notify, baseHeight, extraHeight, woodMeta, leavesMeta);
    }

    @Override
    public boolean generate(World world, Random random, int x, int y, int z) {
        int height = func_150533_a(random);
        if (!func_150537_a(world, random, x, y, z, height)) return false;
        crown(world, x, z, y + height, 2, random);
        for (int by = y + height - 2 - random.nextInt(4); by > y + height / 2; by -= 2 + random.nextInt(4)) {
            float angle = random.nextFloat() * (float) Math.PI * 2.0F;
            int bx = x + (int) (0.5F + Mth.cos(angle) * 4.0F);
            int bz = z + (int) (0.5F + Mth.sin(angle) * 4.0F);
            for (int i = 0; i < 5; i++) {
                bx = x + (int) (1.5F + Mth.cos(angle) * i);
                bz = z + (int) (1.5F + Mth.sin(angle) * i);
                setBlockAndNotifyAdequately(world, bx, by - 3 + i / 2, bz, Blocks.log, woodMetadata);
            }
            int clump = 1 + random.nextInt(2);
            for (int ly = by - clump; ly <= by; ly++) func_150534_b(world, bx, ly, bz, 1 - (ly - by), random);
        }
        for (int i = 0; i < height; i++) {
            int ly = y + i;
            if (LegacyFeatures.airOrLeaves(world, x, ly, z)) {
                setBlockAndNotifyAdequately(world, x, ly, z, Blocks.log, woodMetadata);
                if (i > 0) {
                    vine(world, random, x - 1, ly, z, 8);
                    vine(world, random, x, ly, z - 1, 1);
                }
            }
            if (i < height - 1) {
                if (LegacyFeatures.airOrLeaves(world, x + 1, ly, z)) {
                    setBlockAndNotifyAdequately(world, x + 1, ly, z, Blocks.log, woodMetadata);
                    if (i > 0) {
                        vine(world, random, x + 2, ly, z, 2);
                        vine(world, random, x + 1, ly, z - 1, 1);
                    }
                }
                if (LegacyFeatures.airOrLeaves(world, x + 1, ly, z + 1)) {
                    setBlockAndNotifyAdequately(world, x + 1, ly, z + 1, Blocks.log, woodMetadata);
                    if (i > 0) {
                        vine(world, random, x + 2, ly, z + 1, 2);
                        vine(world, random, x + 1, ly, z + 2, 4);
                    }
                }
                if (LegacyFeatures.airOrLeaves(world, x, ly, z + 1)) {
                    setBlockAndNotifyAdequately(world, x, ly, z + 1, Blocks.log, woodMetadata);
                    if (i > 0) {
                        vine(world, random, x - 1, ly, z + 1, 8);
                        vine(world, random, x, ly, z + 2, 4);
                    }
                }
            }
        }
        return true;
    }

    private void vine(World world, Random random, int x, int y, int z, int meta) {
        if (random.nextInt(3) > 0 && world.isAirBlock(x, y, z)) setBlockAndNotifyAdequately(world, x, y, z, Blocks.vine, meta);
    }

    /** Three leaf discs at the top of the trunk, growing wider downwards. */
    private void crown(World world, int x, int z, int top, int radius, Random random) {
        for (int ly = top - 2; ly <= top; ly++) func_150535_a(world, x, ly, z, radius + 1 - (ly - top), random);
    }
}
