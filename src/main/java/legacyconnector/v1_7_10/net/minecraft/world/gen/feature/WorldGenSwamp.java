package legacyconnector.v1_7_10.net.minecraft.world.gen.feature;

import java.util.Random;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/** The vine-hung oak of swamps (it may stand in shallow water): the modern swamp oak feature. */
public class WorldGenSwamp extends WorldGenAbstractTree {
    public WorldGenSwamp() {
        super(false);
    }

    @Override
    public boolean generate(World world, Random random, int x, int y, int z) {
        return ModernFeature.place(world, "swamp_oak", random, x, y, z);
    }
}
