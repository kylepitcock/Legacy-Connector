package legacyconnector.v1_7_10.net.minecraft.world.gen.feature;

import java.util.Random;
import legacyconnector.v1_7_10.net.minecraft.block.Block;
import legacyconnector.v1_7_10.net.minecraft.init.Blocks;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/** Scatters two-block-tall plants (tall grass, sunflowers, roses, peonies ...): vanilla's double plant generator. */
public class WorldGenDoublePlant extends WorldGenerator {
    private int field_150549_a;

    /** func_150548_a: the plant type, the lower half's metadata. */
    public void func_150548_a(int type) {
        this.field_150549_a = type;
    }

    @Override
    public boolean generate(World world, Random random, int x, int y, int z) {
        boolean placed = false;
        Block plant = Blocks.double_plant;
        for (int i = 0; i < 64; i++) {
            int px = x + random.nextInt(8) - random.nextInt(8);
            int py = y + random.nextInt(4) - random.nextInt(4);
            int pz = z + random.nextInt(8) - random.nextInt(8);
            if (world.isAirBlock(px, py, pz) && (!World.mcc$get$provider(world).hasNoSky || py < 254) && plant.canPlaceBlockAt(world, px, py, pz)) {
                world.setBlock(px, py, pz, plant, field_150549_a, 2);
                world.setBlock(px, py + 1, pz, plant, 8, 2);
                placed = true;
            }
        }
        return placed;
    }
}
