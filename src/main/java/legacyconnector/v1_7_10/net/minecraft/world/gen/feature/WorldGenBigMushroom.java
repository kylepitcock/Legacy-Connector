package legacyconnector.v1_7_10.net.minecraft.world.gen.feature;

import java.util.Random;
import legacyconnector.v1_7_10.net.minecraft.block.Block;
import legacyconnector.v1_7_10.net.minecraft.init.Blocks;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/**
 * A huge mushroom: type 0 brown (flat cap), 1 red (dome), -1 either. The cap and stem metadata (which faces show the
 * cap texture) become the modern mushroom block faces through the legacy block state table.
 */
public class WorldGenBigMushroom extends WorldGenerator {
    public int mushroomType = -1;

    public WorldGenBigMushroom(int type) {
        super(true);
        this.mushroomType = type;
    }

    public WorldGenBigMushroom() {
        super(false);
    }

    @Override
    public boolean generate(World world, Random random, int x, int y, int z) {
        int type = random.nextInt(2);
        if (mushroomType >= 0) type = mushroomType;
        int height = random.nextInt(3) + 4;
        if (y < 1 || y + height + 1 >= 256) return false;
        for (int by = y; by <= y + 1 + height; by++) {
            int r = by <= y + 3 ? 0 : 3;
            for (int bx = x - r; bx <= x + r; bx++) {
                for (int bz = z - r; bz <= z + r; bz++) {
                    if (by < 0 || by >= 256) return false;
                    Block b = world.getBlock(bx, by, bz);
                    if (!b.isAir(world, bx, by, bz) && !b.isLeaves(world, bx, by, bz)) return false;
                }
            }
        }
        Block soil = world.getBlock(x, y - 1, z);
        if (soil != Blocks.dirt && soil != Blocks.grass && soil != Blocks.mycelium) return false;
        Block cap = type == 0 ? Blocks.brown_mushroom_block : Blocks.red_mushroom_block;
        int top = y + height;
        for (int by = type == 1 ? top - 3 : top; by <= top; by++) {
            int r = by < top ? 2 : 1;
            if (type == 0) r = 3;
            for (int bx = x - r; bx <= x + r; bx++) {
                for (int bz = z - r; bz <= z + r; bz++) {
                    int meta = 5;
                    if (bx == x - r) meta--;
                    if (bx == x + r) meta++;
                    if (bz == z - r) meta -= 3;
                    if (bz == z + r) meta += 3;
                    if (type == 0 || by < top) {
                        if ((bx == x - r || bx == x + r) && (bz == z - r || bz == z + r)) continue;
                        if (bx == x - (r - 1) && bz == z - r) meta = 1;
                        if (bx == x - r && bz == z - (r - 1)) meta = 1;
                        if (bx == x + (r - 1) && bz == z - r) meta = 3;
                        if (bx == x + r && bz == z - (r - 1)) meta = 3;
                        if (bx == x - (r - 1) && bz == z + r) meta = 7;
                        if (bx == x - r && bz == z + (r - 1)) meta = 7;
                        if (bx == x + (r - 1) && bz == z + r) meta = 9;
                        if (bx == x + r && bz == z + (r - 1)) meta = 9;
                    }
                    if (meta == 5 && by < top) meta = 0;
                    if ((meta != 0 || y >= top - 1) && world.getBlock(bx, by, bz).canBeReplacedByLeaves(world, bx, by, bz)) {
                        setBlockAndNotifyAdequately(world, bx, by, bz, cap, meta);
                    }
                }
            }
        }
        for (int i = 0; i < height; i++) {
            if (world.getBlock(x, y + i, z).canBeReplacedByLeaves(world, x, y + i, z)) setBlockAndNotifyAdequately(world, x, y + i, z, cap, 10);
        }
        return true;
    }
}
