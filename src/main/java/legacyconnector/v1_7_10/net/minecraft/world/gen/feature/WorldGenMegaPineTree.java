package legacyconnector.v1_7_10.net.minecraft.world.gen.feature;

import java.util.Random;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/** The 2x2 spruce of mega taigas (the bushier mega spruce with {@code spruce}): the modern mega pine and mega spruce features. */
public class WorldGenMegaPineTree extends WorldGenHugeTrees {
    private final boolean field_150542_e;

    public WorldGenMegaPineTree(boolean notify, boolean spruce) {
        super(notify, 13, 15, 1, 1);
        this.field_150542_e = spruce;
    }

    @Override
    public boolean generate(World world, Random random, int x, int y, int z) {
        return ModernFeature.place(world, field_150542_e ? "mega_spruce" : "mega_pine", random, x, y, z);
    }
}
