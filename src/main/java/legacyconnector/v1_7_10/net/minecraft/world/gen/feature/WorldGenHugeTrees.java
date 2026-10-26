package legacyconnector.v1_7_10.net.minecraft.world.gen.feature;

import java.util.Random;
import legacyconnector.v1_7_10.net.minecraft.init.Blocks;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/**
 * Base of the 2x2 trees (mega jungle and mega pine): the trunk height roll, the room and soil check, and the leaf discs mods build
 * their own big trees from.
 */
public abstract class WorldGenHugeTrees extends WorldGenAbstractTree {
    protected final int baseHeight;
    protected final int woodMetadata;
    protected final int leavesMetadata;
    /** The random extra trunk height (0..n-1 added to the base). */
    protected int field_150538_d;

    public WorldGenHugeTrees(boolean notify, int baseHeight, int extraHeight, int woodMeta, int leavesMeta) {
        super(notify);
        this.baseHeight = baseHeight;
        this.field_150538_d = extraHeight;
        this.woodMetadata = woodMeta;
        this.leavesMetadata = leavesMeta;
    }

    /** The height of one tree: base + 0..2 + the random extra. */
    protected int func_150533_a(Random random) {
        int h = random.nextInt(3) + baseHeight;
        if (field_150538_d > 1) h += random.nextInt(field_150538_d);
        return h;
    }

    /** Whether the column around the 2x2 trunk is free up to the top. */
    private boolean func_150536_b(World world, Random random, int x, int y, int z, int height) {
        if (y < 1 || y + height + 1 > 256) return false;
        for (int ly = y; ly <= y + 1 + height; ly++) {
            int r = ly == y ? 1 : 2;
            if (ly >= y + 1 + height - 2) r = 2;
            for (int lx = x - r; lx <= x + r; lx++) {
                for (int lz = z - r; lz <= z + r; lz++) {
                    if (ly < 0 || ly >= 256 || !isReplaceable(world, lx, ly, lz)) return false;
                }
            }
        }
        return true;
    }

    /** Whether the four soil blocks hold a tree; if so they are told a tree grew on them. */
    private boolean func_150532_c(World world, Random random, int x, int y, int z) {
        if (y < 2 || !LegacyFeatures.sustainsTree(world, x, y - 1, z)) return false;
        LegacyFeatures.plantGrew(world, x, y - 1, z, x, y, z);
        LegacyFeatures.plantGrew(world, x + 1, y - 1, z, x, y, z);
        LegacyFeatures.plantGrew(world, x, y - 1, z + 1, x, y, z);
        LegacyFeatures.plantGrew(world, x + 1, y - 1, z + 1, x, y, z);
        return true;
    }

    /** Room and soil for a tree of the height at the position. */
    protected boolean func_150537_a(World world, Random random, int x, int y, int z, int height) {
        return func_150536_b(world, random, x, y, z, height) && func_150532_c(world, random, x, y, z);
    }

    /** A leaf disc around a 2x2 trunk (centred between the four logs). */
    protected void func_150535_a(World world, int x, int y, int z, int radius, Random random) {
        int r2 = radius * radius;
        for (int lx = x - radius; lx <= x + radius + 1; lx++) {
            int dx = lx - x;
            for (int lz = z - radius; lz <= z + radius + 1; lz++) {
                int dz = lz - z;
                int ex = dx - 1;
                int ez = dz - 1;
                if (dx * dx + dz * dz <= r2 || ex * ex + ez * ez <= r2 || dx * dx + ez * ez <= r2 || ex * ex + dz * dz <= r2) {
                    if (LegacyFeatures.airOrLeaves(world, lx, y, lz)) setBlockAndNotifyAdequately(world, lx, y, lz, Blocks.leaves, leavesMetadata);
                }
            }
        }
    }

    /** A leaf disc around one log. */
    protected void func_150534_b(World world, int x, int y, int z, int radius, Random random) {
        int r2 = radius * radius;
        for (int lx = x - radius; lx <= x + radius; lx++) {
            int dx = lx - x;
            for (int lz = z - radius; lz <= z + radius; lz++) {
                int dz = lz - z;
                if (dx * dx + dz * dz <= r2 && LegacyFeatures.airOrLeaves(world, lx, y, lz)) {
                    setBlockAndNotifyAdequately(world, lx, y, lz, Blocks.leaves, leavesMetadata);
                }
            }
        }
    }
}
