package mccompat.v1_7_10.net.minecraft.world.gen.feature;

import java.util.Random;
import mccompat.v1_7_10.net.minecraft.block.Block;
import mccompat.v1_7_10.net.minecraft.world.World;

/** Scatters a plant block: vanilla's flower generator (64 attempts around the origin). */
public class WorldGenFlowers extends WorldGenerator {
    private Block field_150552_a;
    private int field_150551_b;

    public WorldGenFlowers(Block block) {
        this.field_150552_a = block;
        this.field_150551_b = 0;
    }

    /** func_150550_a: sets the plant block and its metadata. */
    public void func_150550_a(Block block, int meta) {
        this.field_150552_a = block;
        this.field_150551_b = meta;
    }

    @Override
    public boolean generate(World world, Random random, int x, int y, int z) {
        for (int i = 0; i < 64; i++) {
            int px = x + random.nextInt(8) - random.nextInt(8);
            int py = y + random.nextInt(4) - random.nextInt(4);
            int pz = z + random.nextInt(8) - random.nextInt(8);
            if (world.isAirBlock(px, py, pz) && (!world.provider.hasNoSky || py < 255) && field_150552_a.canBlockStay(world, px, py, pz)) {
                world.setBlock(px, py, pz, field_150552_a, field_150551_b, 2);
            }
        }
        return true;
    }
}
