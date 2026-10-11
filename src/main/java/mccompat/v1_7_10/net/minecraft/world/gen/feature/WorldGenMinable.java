package mccompat.v1_7_10.net.minecraft.world.gen.feature;

import java.util.Random;
import mccompat.v1_7_10.net.minecraft.block.Block;
import mccompat.v1_7_10.net.minecraft.init.Blocks;
import mccompat.v1_7_10.net.minecraft.util.MathHelper;
import mccompat.v1_7_10.net.minecraft.world.World;

/** Vanilla 1.7.10 ore vein generator, ported line for line. */
public class WorldGenMinable extends WorldGenerator {
    private final Block field_150519_a;
    private final int numberOfBlocks;
    private final Block field_150518_c;
    private int mineableBlockMeta;

    public WorldGenMinable(Block block, int count) {
        this(block, count, Blocks.stone);
    }

    public WorldGenMinable(Block block, int count, Block target) {
        this.field_150519_a = block;
        this.numberOfBlocks = count;
        this.field_150518_c = target;
    }

    public WorldGenMinable(Block block, int meta, int count, Block target) {
        this(block, count, target);
        this.mineableBlockMeta = meta;
    }

    @Override
    public boolean generate(World world, Random rand, int x, int y, int z) {
        float f = rand.nextFloat() * (float) Math.PI;
        double d0 = x + 8 + MathHelper.sin(f) * numberOfBlocks / 8.0F;
        double d1 = x + 8 - MathHelper.sin(f) * numberOfBlocks / 8.0F;
        double d2 = z + 8 + MathHelper.cos(f) * numberOfBlocks / 8.0F;
        double d3 = z + 8 - MathHelper.cos(f) * numberOfBlocks / 8.0F;
        double d4 = y + rand.nextInt(3) - 2;
        double d5 = y + rand.nextInt(3) - 2;
        for (int l = 0; l <= numberOfBlocks; ++l) {
            double d6 = d0 + (d1 - d0) * l / numberOfBlocks;
            double d7 = d4 + (d5 - d4) * l / numberOfBlocks;
            double d8 = d2 + (d3 - d2) * l / numberOfBlocks;
            double d9 = rand.nextDouble() * numberOfBlocks / 16.0D;
            double d10 = (MathHelper.sin(l * (float) Math.PI / numberOfBlocks) + 1.0F) * d9 + 1.0D;
            double d11 = (MathHelper.sin(l * (float) Math.PI / numberOfBlocks) + 1.0F) * d9 + 1.0D;
            int i1 = MathHelper.floor_double(d6 - d10 / 2.0D);
            int j1 = MathHelper.floor_double(d7 - d11 / 2.0D);
            int k1 = MathHelper.floor_double(d8 - d10 / 2.0D);
            int l1 = MathHelper.floor_double(d6 + d10 / 2.0D);
            int i2 = MathHelper.floor_double(d7 + d11 / 2.0D);
            int j2 = MathHelper.floor_double(d8 + d10 / 2.0D);
            for (int k2 = i1; k2 <= l1; ++k2) {
                double d12 = (k2 + 0.5D - d6) / (d10 / 2.0D);
                if (d12 * d12 >= 1.0D) continue;
                for (int l2 = j1; l2 <= i2; ++l2) {
                    double d13 = (l2 + 0.5D - d7) / (d11 / 2.0D);
                    if (d12 * d12 + d13 * d13 >= 1.0D) continue;
                    for (int i3 = k1; i3 <= j2; ++i3) {
                        double d14 = (i3 + 0.5D - d8) / (d10 / 2.0D);
                        if (d12 * d12 + d13 * d13 + d14 * d14 < 1.0D) {
                            Block existing = world.getBlock(k2, l2, i3);
                            if (existing != null && existing.isReplaceableOreGen(world, k2, l2, i3, field_150518_c)) {
                                world.setBlock(k2, l2, i3, field_150519_a, mineableBlockMeta, 2);
                            }
                        }
                    }
                }
            }
        }
        return true;
    }
}
