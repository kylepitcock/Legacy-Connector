package legacyconnector.v1_7_10.net.minecraft.world.gen.feature;

import java.util.Random;
import legacyconnector.v1_7_10.net.minecraft.block.Block;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/** A few dead bushes (or the given plant) on the ground below the origin. */
public class WorldGenDeadBush extends WorldGenerator {
    private Block field_150547_a;
    /** The block of a 1.6.4 id constructor, looked up when generating (1.6.4 read {@code blocksList[id]} then). */
    private java.util.function.Supplier<Block> mcc$byId;

    public WorldGenDeadBush(Block plant) {
        this.field_150547_a = plant;
    }

    /** 1.6.4: {@code new WorldGenDeadBush(blockId)}. */
    public WorldGenDeadBush(int blockId) {
        this.mcc$byId = legacyconnector.v1_6_4.net.minecraft.util.Ids.lazyBlock(blockId);
    }

    @Override
    public boolean generate(World world, Random random, int x, int y, int z) {
        if (field_150547_a == null && mcc$byId != null) field_150547_a = mcc$byId.get();
        if (field_150547_a == null) return false;
        while (y > 0 && LegacyFeatures.airOrLeaves(world, x, y, z)) y--;
        for (int i = 0; i < 4; i++) {
            int px = x + random.nextInt(8) - random.nextInt(8);
            int py = y + random.nextInt(4) - random.nextInt(4);
            int pz = z + random.nextInt(8) - random.nextInt(8);
            if (world.isAirBlock(px, py, pz) && field_150547_a.canBlockStay(world, px, py, pz)) world.setBlock(px, py, pz, field_150547_a, 0, 2);
        }
        return true;
    }
}
