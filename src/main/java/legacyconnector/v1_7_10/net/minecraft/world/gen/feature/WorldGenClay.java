package legacyconnector.v1_7_10.net.minecraft.world.gen.feature;

import java.util.Random;
import legacyconnector.v1_7_10.net.minecraft.block.material.Material;
import legacyconnector.v1_7_10.net.minecraft.init.Blocks;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/** A disc of clay under shallow water; the decorator's {@code clayGen}. */
public class WorldGenClay extends WorldGenerator {
    private final int numberOfBlocks;

    public WorldGenClay(int numberOfBlocks) {
        this.numberOfBlocks = numberOfBlocks;
    }

    @Override
    public boolean generate(World world, Random random, int x, int y, int z) {
        if (world.getBlock(x, y, z).getMaterial() != Material.water) return false;
        int r = random.nextInt(numberOfBlocks - 2) + 2;
        for (int px = x - r; px <= x + r; px++) {
            for (int pz = z - r; pz <= z + r; pz++) {
                int dx = px - x;
                int dz = pz - z;
                if (dx * dx + dz * dz > r * r) continue;
                for (int py = y - 1; py <= y + 1; py++) {
                    var b = world.getBlock(px, py, pz);
                    if (b == Blocks.dirt || b == Blocks.clay) world.setBlock(px, py, pz, Blocks.clay, 0, 2);
                }
            }
        }
        return true;
    }
}
