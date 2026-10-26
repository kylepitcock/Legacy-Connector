package legacyconnector.v1_7_10.net.minecraft.world.gen.feature;

import java.util.Random;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/** A birch tree (the tall variety of birch forests with {@code superBirch}): the modern birch feature. */
public class WorldGenForest extends WorldGenAbstractTree {
    private final boolean field_150531_a;

    public WorldGenForest(boolean notify, boolean superBirch) {
        super(notify);
        this.field_150531_a = superBirch;
    }

    @Override
    public boolean generate(World world, Random random, int x, int y, int z) {
        return ModernFeature.place(world, field_150531_a ? "super_birch_bees_0002" : "birch", random, x, y, z);
    }
}
