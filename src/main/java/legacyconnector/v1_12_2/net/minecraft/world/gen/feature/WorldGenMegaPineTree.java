package legacyconnector.v1_12_2.net.minecraft.world.gen.feature;

import java.util.Random;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/** 1.12.2's huge pine or spruce: the modern game's own feature (its successor) is placed. */
public class WorldGenMegaPineTree extends WorldGenHugeTrees {
    private boolean useBaseHeight;

    public WorldGenMegaPineTree(boolean notify, boolean useBaseHeightIn) {
        super(notify, 13, 15, null, null);
        this.useBaseHeight = useBaseHeightIn;
    }

    @Override
    public boolean generate(World worldIn, Random rand, BlockPos position) {
        return org.kylepitcock.legacyconnector.v1_12_2.bridge.ModernFeatures1122.place(worldIn, useBaseHeight ? net.minecraft.data.worldgen.features.TreeFeatures.MEGA_PINE : net.minecraft.data.worldgen.features.TreeFeatures.MEGA_SPRUCE, rand, position);
    }
}
