package legacyconnector.v1_12_2.net.minecraft.world.gen.feature;

import java.util.Random;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/** 1.12.2 End gateway: a gateway block in its bedrock frame (the modern gateway feature; its exit is searched on first use). */
public class WorldGenEndGateway extends WorldGenerator {
    public WorldGenEndGateway() {
        super(false);
    }

    @Override
    public boolean generate(World worldIn, Random rand, BlockPos position) {
        return org.kylepitcock.legacyconnector.v1_12_2.bridge.ModernFeatures1122.placeFeature(worldIn,
                net.minecraft.world.level.levelgen.feature.Feature.END_GATEWAY,
                net.minecraft.world.level.levelgen.feature.configurations.EndGatewayConfiguration.delayedExitSearch(), rand, position);
    }
}
