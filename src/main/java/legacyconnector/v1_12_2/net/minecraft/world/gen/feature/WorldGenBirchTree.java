package legacyconnector.v1_12_2.net.minecraft.world.gen.feature;

import java.util.Random;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/** 1.12.2's birch (the tall one with extra height): the modern game's own feature (its successor) is placed. */
public class WorldGenBirchTree extends WorldGenAbstractTree {
    private boolean useExtraRandomHeight;

    public WorldGenBirchTree(boolean notify, boolean useExtraRandomHeightIn) {
        super(notify);
        this.useExtraRandomHeight = useExtraRandomHeightIn;
    }

    @Override
    public boolean generate(World worldIn, Random rand, BlockPos position) {
        return org.kylepitcock.legacyconnector.v1_12_2.bridge.ModernFeatures1122.place(worldIn, useExtraRandomHeight ? net.minecraft.data.worldgen.features.TreeFeatures.SUPER_BIRCH_BEES_0002 : net.minecraft.data.worldgen.features.TreeFeatures.BIRCH, rand, position);
    }
}
