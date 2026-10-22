package legacyconnector.v1_7_10.net.minecraft.block;

import java.util.Random;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/** Blocks bone meal can grow. */
public interface IGrowable {
    boolean canFertilize(World world, int x, int y, int z, boolean isClient);

    boolean shouldFertilize(World world, Random random, int x, int y, int z);

    void fertilize(World world, Random random, int x, int y, int z);
}
