package legacyconnector.v1_7_10.net.minecraft.world.gen.feature;

import java.util.Random;
import legacyconnector.v1_7_10.net.minecraft.block.Block;
import legacyconnector.v1_7_10.net.minecraft.block.material.Material;
import legacyconnector.v1_7_10.net.minecraft.init.Blocks;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/** A disc of sand or gravel in shallow water (vanilla's beach and river-bed patches); the decorator's {@code sandGen}/{@code gravelAsSandGen}. */
public class WorldGenSand extends WorldGenerator {
    private final Block block;
    private final int radius;

    public WorldGenSand(Block block, int radius) {
        this.block = block;
        this.radius = radius;
    }

    @Override
    public boolean generate(World world, Random random, int x, int y, int z) {
        if (world.getBlock(x, y, z).getMaterial() != Material.water) return false;
        int r = random.nextInt(radius - 2) + 2;
        for (int px = x - r; px <= x + r; px++) {
            for (int pz = z - r; pz <= z + r; pz++) {
                int dx = px - x;
                int dz = pz - z;
                if (dx * dx + dz * dz > r * r) continue;
                for (int py = y - 2; py <= y + 2; py++) {
                    Block b = world.getBlock(px, py, pz);
                    if (b == Blocks.dirt || b == Blocks.grass) world.setBlock(px, py, pz, block, 0, 2);
                }
            }
        }
        return true;
    }
}
