package legacyconnector.v1_12_2.net.minecraft.world.gen.feature;

import java.util.Random;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/** 1.12.2's acacia: the modern game's own feature (its successor) is placed. */
public class WorldGenSavannaTree extends WorldGenAbstractTree {
    public WorldGenSavannaTree(boolean notify) {
        super(notify);
    }

    @Override
    public boolean generate(World worldIn, Random rand, BlockPos position) {
        return org.kylepitcock.legacyconnector.v1_12_2.bridge.ModernFeatures1122.place(worldIn, net.minecraft.data.worldgen.features.TreeFeatures.ACACIA, rand, position);
    }
}
