package legacyconnector.v1_7_10.net.minecraft.world.gen.feature;

import java.util.Random;
import legacyconnector.v1_7_10.net.minecraft.block.Block;
import legacyconnector.v1_7_10.net.minecraft.init.Blocks;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/** A lake of {@code block}: a blob of blobs hollowed into the ground (water and lava lakes, mods' tar and oil lakes). */
public class WorldGenLakes extends WorldGenerator {
    private Block field_150556_a;
    /** The block of a 1.6.4 id constructor, looked up when generating (1.6.4 read {@code blocksList[id]} then). */
    private java.util.function.Supplier<Block> mcc$byId;

    public WorldGenLakes(Block block) {
        this.field_150556_a = block;
    }

    /** 1.6.4: {@code new WorldGenLakes(blockId)}. */
    public WorldGenLakes(int blockId) {
        this.mcc$byId = legacyconnector.v1_6_4.net.minecraft.util.Ids.lazyBlock(blockId);
    }

    @Override
    public boolean generate(World world, Random random, int x, int y, int z) {
        if (field_150556_a == null && mcc$byId != null) field_150556_a = mcc$byId.get();
        if (field_150556_a == null) return false;
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
