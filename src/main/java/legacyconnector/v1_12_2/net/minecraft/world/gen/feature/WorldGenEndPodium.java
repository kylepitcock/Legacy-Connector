package legacyconnector.v1_12_2.net.minecraft.world.gen.feature;

import java.util.Random;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/** 1.12.2 exit portal podium of the End (the modern game's own podium feature, active or not, is placed). */
public class WorldGenEndPodium extends WorldGenerator {
    /** Where the podium stands: the End's origin column. */
    public static final BlockPos END_PODIUM_LOCATION = BlockPos.ORIGIN;
    private final boolean activePortal;

    public WorldGenEndPodium(boolean activePortalIn) {
        super(false);
        this.activePortal = activePortalIn;
    }

    @Override
    public boolean generate(World worldIn, Random rand, BlockPos position) {
        return org.kylepitcock.legacyconnector.v1_12_2.bridge.ModernFeatures1122.placeFeature(worldIn,
                new net.minecraft.world.level.levelgen.feature.EndPodiumFeature(activePortal),
                net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration.NONE, rand, position);
    }
}
