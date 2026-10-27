package legacyconnector.v1_12_2.net.minecraft.world.gen.feature;

import java.util.Random;
import legacyconnector.v1_12_2.net.minecraft.block.Block;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/** 1.12.2 sand/gravel disk under water: dirt and grass within a random radius become the block (as vanilla). */
public class WorldGenSand extends WorldGenerator {
    private final Block block;
    private final int radius;

    public WorldGenSand(Block block, int radius) {
        super(false);
        this.block = block;
        this.radius = radius;
    }

    @Override
    public boolean generate(World worldIn, Random rand, BlockPos position) {
        if (block == null || radius <= 2) return false;
        var here = org.kylepitcock.legacyconnector.v1_12_2.bridge.StatesBridge1122.toModern(worldIn.getBlockState(position));
        if (!here.getFluidState().is(net.minecraft.tags.FluidTags.WATER)) return false;
        int r = rand.nextInt(radius - 2) + 2;
        for (int x = position.getX() - r; x <= position.getX() + r; ++x) {
            for (int z = position.getZ() - r; z <= position.getZ() + r; ++z) {
                int dx = x - position.getX();
                int dz = z - position.getZ();
                if (dx * dx + dz * dz > r * r) continue;
                for (int y = position.getY() - 2; y <= position.getY() + 2; ++y) {
                    BlockPos p = new BlockPos(x, y, z);
                    var m = org.kylepitcock.legacyconnector.v1_12_2.bridge.StatesBridge1122.toModern(worldIn.getBlockState(p));
                    if (m.is(net.minecraft.world.level.block.Blocks.DIRT) || m.is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK)) {
                        worldIn.setBlockState(p, block.getDefaultState(), 2);
                    }
                }
            }
        }
        return true;
    }
}
