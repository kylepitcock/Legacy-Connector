package legacyconnector.v1_7_10.net.minecraft.world.gen.feature;

import java.util.Random;
import legacyconnector.v1_7_10.net.minecraft.init.Blocks;
import legacyconnector.v1_7_10.net.minecraft.world.World;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.VineBlock;

/**
 * Vines up a column to y=128: in each air block a vine attaches to the first wall it can hold to; at a solid block the column moves
 * a few blocks sideways (jungles and swamps).
 */
public class WorldGenVines extends WorldGenerator {
    /** Legacy vine metadata for a wall to the south, west, north and east (sides 2..5 of the vine block). */
    private static final int[] META = {1, 4, 8, 2};
    private static final Direction[] WALL = {Direction.SOUTH, Direction.NORTH, Direction.EAST, Direction.WEST};

    @Override
    public boolean generate(World world, Random random, int x, int y, int z) {
        int startX = x;
        int startZ = z;
        var level = world.mcc$level();
        for (; y < 128; y++) {
            if (world.isAirBlock(x, y, z)) {
                for (int side = 0; side < 4; side++) {
                    Direction wall = WALL[side];
                    BlockPos neighbour = new BlockPos(x + wall.getStepX(), y, z + wall.getStepZ());
                    boolean holds;
                    try {
                        holds = level != null && VineBlock.isAcceptableNeighbour(level, neighbour, wall);
                    } catch (RuntimeException e) {
                        holds = false;
                    }
                    if (holds) {
                        world.setBlock(x, y, z, Blocks.vine, META[side], 2);
                        break;
                    }
                }
            } else {
                x = startX + random.nextInt(4) - random.nextInt(4);
                z = startZ + random.nextInt(4) - random.nextInt(4);
            }
        }
        return true;
    }
}
