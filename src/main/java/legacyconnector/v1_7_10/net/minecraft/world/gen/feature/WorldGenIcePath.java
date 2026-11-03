package legacyconnector.v1_7_10.net.minecraft.world.gen.feature;

import java.util.Random;
import legacyconnector.v1_7_10.net.minecraft.block.Block;
import legacyconnector.v1_7_10.net.minecraft.init.Blocks;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/** A disc of packed ice laid over the snow surface (the ice plains spikes biome's paths). */
public class WorldGenIcePath extends WorldGenerator {
    private final Block field_150555_a = Blocks.packed_ice;
    private final int basePathWidth;

    public WorldGenIcePath(int width) {
        this.basePathWidth = width;
    }

    @Override
    public boolean generate(World world, Random random, int x, int y, int z) {
        while (world.isAirBlock(x, y, z) && y > 2) --y;
        if (world.getBlock(x, y, z) != Blocks.snow) return false;
        int radius = random.nextInt(Math.max(1, basePathWidth - 2)) + 2;
        for (int px = x - radius; px <= x + radius; px++) {
            for (int pz = z - radius; pz <= z + radius; pz++) {
                int dx = px - x;
                int dz = pz - z;
                if (dx * dx + dz * dz > radius * radius) continue;
                for (int py = y - 1; py <= y + 1; py++) {
                    Block block = world.getBlock(px, py, pz);
                    if (block == Blocks.dirt || block == Blocks.snow || block == Blocks.ice) world.setBlock(px, py, pz, field_150555_a, 0, 2);
                }
            }
        }
        return true;
    }
}
