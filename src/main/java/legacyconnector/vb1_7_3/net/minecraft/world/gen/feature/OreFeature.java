package legacyconnector.vb1_7_3.net.minecraft.world.gen.feature;

import java.util.Random;
import legacyconnector.vb1_7_3.net.minecraft.world.World;

/** Beta 1.7.3 {@code WorldGenMinable}: an ellipsoid vein of one block id through stone. */
public class OreFeature extends Feature {
    private final int blockId;
    private final int size;

    public OreFeature(int blockId, int size) {
        this.blockId = blockId;
        this.size = size;
    }

    @Override
    public boolean generate(World world, Random random, int x, int y, int z) {
        float angle = random.nextFloat() * (float) Math.PI;
        double x1 = x + 8 + Math.sin(angle) * size / 8.0F;
        double x2 = x + 8 - Math.sin(angle) * size / 8.0F;
        double z1 = z + 8 + Math.cos(angle) * size / 8.0F;
        double z2 = z + 8 - Math.cos(angle) * size / 8.0F;
        double y1 = y + random.nextInt(3) + 2;
        double y2 = y + random.nextInt(3) + 2;
        for (int i = 0; i <= size; i++) {
            double cx = x1 + (x2 - x1) * i / size;
            double cy = y1 + (y2 - y1) * i / size;
            double cz = z1 + (z2 - z1) * i / size;
            double scale = random.nextDouble() * size / 16.0D;
            double diameter = (Math.sin(i * (float) Math.PI / size) + 1.0F) * scale + 1.0D;
            int minX = (int) Math.floor(cx - diameter / 2.0D), minY = (int) Math.floor(cy - diameter / 2.0D), minZ = (int) Math.floor(cz - diameter / 2.0D);
            int maxX = (int) Math.floor(cx + diameter / 2.0D), maxY = (int) Math.floor(cy + diameter / 2.0D), maxZ = (int) Math.floor(cz + diameter / 2.0D);
            for (int bx = minX; bx <= maxX; bx++) {
                double dx = (bx + 0.5D - cx) / (diameter / 2.0D);
                if (dx * dx >= 1.0D) continue;
                for (int by = minY; by <= maxY; by++) {
                    double dy = (by + 0.5D - cy) / (diameter / 2.0D);
                    if (dx * dx + dy * dy >= 1.0D) continue;
                    for (int bz = minZ; bz <= maxZ; bz++) {
                        double dz = (bz + 0.5D - cz) / (diameter / 2.0D);
                        if (dx * dx + dy * dy + dz * dz < 1.0D && world.getBlockId(bx, by, bz) == 1) world.setBlock(bx, by, bz, blockId);
                    }
                }
            }
        }
        return true;
    }
}
