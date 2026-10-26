package legacyconnector.v1_7_10.net.minecraft.world.gen.feature;

import java.util.Random;
import legacyconnector.v1_7_10.net.minecraft.block.Block;
import legacyconnector.v1_7_10.net.minecraft.init.Blocks;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/** A jungle bush: one log on the ground with a three-layer mound of leaves (log and leaf metadata as given). */
public class WorldGenShrub extends WorldGenTrees {
    private final int field_150527_b;
    private final int field_150528_a;

    public WorldGenShrub(int woodMeta, int leavesMeta) {
        super(false);
        this.field_150527_b = woodMeta;
        this.field_150528_a = leavesMeta;
    }

    @Override
    public boolean generate(World world, Random random, int x, int y, int z) {
        while (y > 0 && LegacyFeatures.airOrLeaves(world, x, y, z)) y--;
        if (!LegacyFeatures.sustainsTree(world, x, y, z)) return true;
        y++;
        setBlockAndNotifyAdequately(world, x, y, z, Blocks.log, field_150527_b);
        for (int ly = y; ly <= y + 2; ly++) {
            int radius = 2 - (ly - y);
            for (int lx = x - radius; lx <= x + radius; lx++) {
                int dx = lx - x;
                for (int lz = z - radius; lz <= z + radius; lz++) {
                    int dz = lz - z;
                    if (Math.abs(dx) == radius && Math.abs(dz) == radius && random.nextInt(2) == 0) continue;
                    Block b = world.getBlock(lx, ly, lz);
                    if (b.canBeReplacedByLeaves(world, lx, ly, lz)) setBlockAndNotifyAdequately(world, lx, ly, lz, Blocks.leaves, field_150528_a);
                }
            }
        }
        return true;
    }
}
