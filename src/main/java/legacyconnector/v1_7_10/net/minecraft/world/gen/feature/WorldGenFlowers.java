package legacyconnector.v1_7_10.net.minecraft.world.gen.feature;

import java.util.Random;
import java.util.function.Supplier;
import legacyconnector.v1_7_10.net.minecraft.block.Block;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/** Scatters a plant block: vanilla's flower generator (64 attempts around the origin). */
public class WorldGenFlowers extends WorldGenerator {
    private Block field_150552_a;
    private int field_150551_b;
    /** The block of a 1.6.4 id constructor, looked up when generating (see {@link #WorldGenFlowers(int)}). */
    private Supplier<Block> mcc$byId;

    public WorldGenFlowers(Block block) {
        this.field_150552_a = block;
        this.field_150551_b = 0;
    }

    /** 1.6.4: {@code new WorldGenFlowers(blockId)}; the id is resolved at generation, as 1.6.4 read {@code blocksList[id]} then. */
    public WorldGenFlowers(int blockId) {
        this.mcc$byId = legacyconnector.v1_6_4.net.minecraft.util.Ids.lazyBlock(blockId);
    }

    /** func_150550_a: sets the plant block and its metadata. */
    public void func_150550_a(Block block, int meta) {
        this.field_150552_a = block;
        this.field_150551_b = meta;
        this.mcc$byId = null;
    }

    private Block plant() {
        if (field_150552_a == null && mcc$byId != null) field_150552_a = mcc$byId.get();
        return field_150552_a;
    }

    @Override
    public boolean generate(World world, Random random, int x, int y, int z) {
        Block plant = plant();
        if (plant == null) return false;
        for (int i = 0; i < 64; i++) {
            int px = x + random.nextInt(8) - random.nextInt(8);
            int py = y + random.nextInt(4) - random.nextInt(4);
            int pz = z + random.nextInt(8) - random.nextInt(8);
            if (world.isAirBlock(px, py, pz) && (!World.mcc$get$provider(world).hasNoSky || py < 255) && plant.canBlockStay(world, px, py, pz)) {
                world.setBlock(px, py, pz, plant, field_150551_b, 2);
            }
        }
        return true;
    }
}
