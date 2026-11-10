package legacyconnector.vb1_7_3.net.minecraft.world.gen.feature;

import java.util.Random;
import legacyconnector.vb1_7_3.net.minecraft.block.Block;
import legacyconnector.vb1_7_3.net.minecraft.block.material.Material;
import legacyconnector.vb1_7_3.net.minecraft.world.LightType;
import legacyconnector.vb1_7_3.net.minecraft.world.World;

/** Beta lake generation: intersecting ellipsoids, sealed shoreline checks, and lava stone lining. */
public class LakeFeature extends Feature {
    private int waterBlockId;

    public LakeFeature(int blockId) {
        waterBlockId = blockId;
    }

    @Override
    public boolean generate(World world, Random random, int x, int y, int z) {
        x -= 8;
        z -= 8;
        while (y > 0 && world.isAir(x, y, z)) y--;
        y -= 4;
        boolean[] shape = new boolean[2048];
        int blobs = random.nextInt(4) + 4;
        for (int blob = 0; blob < blobs; blob++) {
            double dx = random.nextDouble() * 6.0D + 3.0D;
            double dy = random.nextDouble() * 4.0D + 2.0D;
            double dz = random.nextDouble() * 6.0D + 3.0D;
            double cx = random.nextDouble() * (16.0D - dx - 2.0D) + 1.0D + dx / 2.0D;
            double cy = random.nextDouble() * (8.0D - dy - 4.0D) + 2.0D + dy / 2.0D;
            double cz = random.nextDouble() * (16.0D - dz - 2.0D) + 1.0D + dz / 2.0D;
            for (int px = 1; px < 15; px++) {
                for (int pz = 1; pz < 15; pz++) {
                    for (int py = 1; py < 7; py++) {
                        double rx = (px - cx) / (dx / 2.0D);
                        double ry = (py - cy) / (dy / 2.0D);
                        double rz = (pz - cz) / (dz / 2.0D);
                        if (rx * rx + ry * ry + rz * rz < 1.0D) shape[index(px, py, pz)] = true;
                    }
                }
            }
        }
        // Validate the entire shell before making any writes.
        for (int px = 0; px < 16; px++) {
            for (int pz = 0; pz < 16; pz++) {
                for (int py = 0; py < 8; py++) {
                    if (!boundary(shape, px, py, pz)) continue;
                    Material material = world.getMaterial(x + px, y + py, z + pz);
                    if (py >= 4 && material.isFluid()) return false;
                    if (py < 4 && !material.isSolid() && world.getBlockId(x + px, y + py, z + pz) != waterBlockId) return false;
                }
            }
        }
        for (int px = 0; px < 16; px++) {
            for (int pz = 0; pz < 16; pz++) {
                for (int py = 0; py < 8; py++) {
                    if (shape[index(px, py, pz)]) world.setBlockWithoutNotifyingNeighbors(x + px, y + py, z + pz, py >= 4 ? 0 : waterBlockId);
                }
            }
        }
        for (int px = 0; px < 16; px++) {
            for (int pz = 0; pz < 16; pz++) {
                for (int py = 4; py < 8; py++) {
                    if (shape[index(px, py, pz)] && world.getBlockId(x + px, y + py - 1, z + pz) == 3
                            && world.getBrightness(LightType.SKY, x + px, y + py, z + pz) > 0)
                        world.setBlockWithoutNotifyingNeighbors(x + px, y + py - 1, z + pz, 2);
                }
            }
        }
        if (Block.BLOCKS[waterBlockId].material == Material.LAVA) {
            for (int px = 0; px < 16; px++) {
                for (int pz = 0; pz < 16; pz++) {
                    for (int py = 0; py < 8; py++) {
                        if (boundary(shape, px, py, pz) && (py < 4 || random.nextInt(2) != 0)
                                && world.getMaterial(x + px, y + py, z + pz).isSolid())
                            world.setBlockWithoutNotifyingNeighbors(x + px, y + py, z + pz, 1);
                    }
                }
            }
        }
        return true;
    }

    private static int index(int x, int y, int z) {
        return (x * 16 + z) * 8 + y;
    }

    private static boolean boundary(boolean[] shape, int x, int y, int z) {
        return !shape[index(x, y, z)] && (x < 15 && shape[index(x + 1, y, z)]
                || x > 0 && shape[index(x - 1, y, z)] || z < 15 && shape[index(x, y, z + 1)]
                || z > 0 && shape[index(x, y, z - 1)] || y < 7 && shape[index(x, y + 1, z)]
                || y > 0 && shape[index(x, y - 1, z)]);
    }
}
