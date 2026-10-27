package legacyconnector.v1_12_2.net.minecraft.world.gen.feature;

import java.util.Random;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/** 1.12.2's huge jungle tree: the modern game's own feature (its successor) is placed. */
public class WorldGenMegaJungle extends WorldGenHugeTrees {
    public WorldGenMegaJungle(boolean notify, int baseHeightIn, int extraRandomHeightIn, legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState woodMetadataIn, legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState leavesMetadataIn) {
        super(notify, baseHeightIn, extraRandomHeightIn, woodMetadataIn, leavesMetadataIn);
    }

    @Override
    public boolean generate(World worldIn, Random rand, BlockPos position) {
        return org.kylepitcock.legacyconnector.v1_12_2.bridge.ModernFeatures1122.place(worldIn, net.minecraft.data.worldgen.features.TreeFeatures.MEGA_JUNGLE_TREE, rand, position);
    }
}
