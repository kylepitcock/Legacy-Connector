package legacyconnector.v1_7_10.net.minecraft.world.gen.feature;

import java.util.Random;
import legacyconnector.v1_7_10.net.minecraft.block.Block;
import legacyconnector.v1_7_10.net.minecraft.init.Blocks;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/** A lake of {@code block}: a blob of blobs hollowed into the ground (water and lava lakes, mods' tar and oil lakes). */
public class WorldGenLakes extends WorldGenerator {
    private final Block field_150556_a;

    public WorldGenLakes(Block block) {
        this.field_150556_a = block;
    }

    @Override
    public boolean generate(World world, Random random, int x, int y, int z) {
        x -= 8;
        z -= 8;
        while (y > 5 && world.isAirBlock(x, y, z)) y--;
        if (y <= 4) return false;
        y -= 4;
        boolean[] shape = new boolean[2048];
        int blobs = random.nextInt(4) + 4;
        for (int i = 0; i < blobs; i++) {
            double dx = random.nextDouble() * 6.0 + 3.0;
            double dy = random.nextDouble() * 4.0 + 2.0;
            double dz = random.nextDouble() * 6.0 + 3.0;
            double cx = random.nextDouble() * (16.0 - dx - 2.0) + 1.0 + dx / 2.0;
            double cy = random.nextDouble() * (8.0 - dy - 4.0) + 2.0 + dy / 2.0;
            double cz = random.nextDouble() * (16.0 - dz - 2.0) + 1.0 + dz / 2.0;
            for (int px = 1; px < 15; px++) {
                for (int pz = 1; pz < 15; pz++) {
                    for (int py = 1; py < 7; py++) {
                        double rx = (px - cx) / (dx / 2.0);
                        double ry = (py - cy) / (dy / 2.0);
                        double rz = (pz - cz) / (dz / 2.0);
                        if (rx * rx + ry * ry + rz * rz < 1.0) shape[(px * 16 + pz) * 8 + py] = true;
                    }
                }
            }
        }
        for (int px = 0; px < 16; px++) {
            for (int pz = 0; pz < 16; pz++) {
                for (int py = 0; py < 8; py++) {
                    if (!shape[(px * 16 + pz) * 8 + py]) continue;
                    // the lower part is filled with the fluid, the upper part is cleared
                    world.setBlock(x + px, y + py, z + pz, py >= 4 ? Blocks.air : field_150556_a, 0, 2);
                }
            }
        }
        return true;
    }
}
