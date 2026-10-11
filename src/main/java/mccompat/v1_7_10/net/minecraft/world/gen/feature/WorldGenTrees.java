package mccompat.v1_7_10.net.minecraft.world.gen.feature;

import java.util.Random;
import mccompat.v1_7_10.net.minecraft.world.World;

/** Small oak (or jungle, with vines) tree: the modern tree feature. */
public class WorldGenTrees extends WorldGenAbstractTree {
    private final boolean jungle;

    public WorldGenTrees(boolean notify) {
        this(notify, 4, 0, 0, false);
    }

    public WorldGenTrees(boolean notify, int minHeight, int woodMeta, int leavesMeta, boolean vines) {
        super(notify);
        this.jungle = woodMeta == 3;
    }

    @Override
    public boolean generate(World world, Random random, int x, int y, int z) {
        return ModernFeature.place(world, jungle ? "jungle_tree_no_vine" : "oak", random, x, y, z);
    }
}
