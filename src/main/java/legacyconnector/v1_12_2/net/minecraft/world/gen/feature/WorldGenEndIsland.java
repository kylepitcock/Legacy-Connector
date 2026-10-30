package legacyconnector.v1_12_2.net.minecraft.world.gen.feature;

import java.util.Random;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/** 1.12.2 small floating End island (the modern game's end island feature, its successor). */
public class WorldGenEndIsland extends WorldGenerator {
    public WorldGenEndIsland() {
        super(false);
    }

    @Override
    public boolean generate(World worldIn, Random rand, BlockPos position) {
        return org.kylepitcock.legacyconnector.v1_12_2.bridge.ModernFeatures1122.placeFeature(worldIn,
                net.minecraft.world.level.levelgen.feature.Feature.END_ISLAND,
                net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration.NONE, rand, position);
    }
}
