package legacyconnector.v1_12_2.net.minecraft.world.gen.feature;

import java.util.Random;
import legacyconnector.v1_12_2.net.minecraft.block.Block;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/** 1.12.2's boulder (mossy cobblestone rocks of giant taigas, or the given block). */
public class WorldGenBlockBlob extends WorldGenerator {
    private final Block block;
    private final int startRadius;

    public WorldGenBlockBlob(Block blockIn, int startRadiusIn) {
        super(false);
        this.block = blockIn;
        this.startRadius = startRadiusIn;
    }

    @Override
    public boolean generate(World worldIn, Random rand, BlockPos position) {
        while (true) {
            if (position.getY() <= 3) return false;
            if (!worldIn.isAirBlock(position.down())) {
                var s = mcc$modernAt(worldIn, position.down());
                if (s.is(net.minecraft.tags.BlockTags.DIRT) || s.is(net.minecraft.world.level.block.Blocks.STONE)) break;
            }
            position = position.down();
        }
        int r = startRadius;
        for (int i = 0; r >= 0 && i < 3; ++i) {
            int j = r + rand.nextInt(2);
            int k = r + rand.nextInt(2);
            int l = r + rand.nextInt(2);
            float f = (j + k + l) * 0.333F + 0.5F;
            for (BlockPos p : BlockPos.getAllInBox(position.add(-j, -k, -l), position.add(j, k, l))) {
                if (p.distanceSq(position) <= f * f) worldIn.setBlockState(p, block.getDefaultState(), 4);
            }
            position = position.add(-(r + 1) + rand.nextInt(2 + r * 2), -rand.nextInt(2), -(r + 1) + rand.nextInt(2 + r * 2));
        }
        return true;
    }
}
