package legacyconnector.v1_12_2.net.minecraft.world.gen.feature;

import java.util.Random;
import legacyconnector.v1_12_2.net.minecraft.block.Block;
import legacyconnector.v1_12_2.net.minecraft.block.material.Material;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/** 1.12.2's lake of a fluid block (water, lava or a mod fluid): the same shape, grass around it, a stone rim for lava. */
public class WorldGenLakes extends WorldGenerator {
    private final Block block;

    public WorldGenLakes(Block blockIn) {
        this.block = blockIn;
    }

    private static Material material(World w, BlockPos p) {
        IBlockState s = w.getBlockState(p);
        return s == null ? Material.AIR : s.getMaterial();
    }

    @Override
    public boolean generate(World worldIn, Random rand, BlockPos position) {
        if (block == null) return false;
        for (position = position.add(-8, 0, -8); position.getY() > 5 && worldIn.isAirBlock(position); position = position.down()) {
        }
        if (position.getY() <= 4) return false;
        position = position.down(4);
        boolean[] lake = new boolean[2048];
        int count = rand.nextInt(4) + 4;
        for (int j = 0; j < count; ++j) {
            double d0 = rand.nextDouble() * 6.0D + 3.0D;
            double d1 = rand.nextDouble() * 4.0D + 2.0D;
            double d2 = rand.nextDouble() * 6.0D + 3.0D;
            double d3 = rand.nextDouble() * (16.0D - d0 - 2.0D) + 1.0D + d0 / 2.0D;
            double d4 = rand.nextDouble() * (8.0D - d1 - 4.0D) + 2.0D + d1 / 2.0D;
            double d5 = rand.nextDouble() * (16.0D - d2 - 2.0D) + 1.0D + d2 / 2.0D;
            for (int l = 1; l < 15; ++l) {
                for (int i1 = 1; i1 < 15; ++i1) {
                    for (int j1 = 1; j1 < 7; ++j1) {
                        double d6 = (l - d3) / (d0 / 2.0D);
                        double d7 = (j1 - d4) / (d1 / 2.0D);
                        double d8 = (i1 - d5) / (d2 / 2.0D);
                        if (d6 * d6 + d7 * d7 + d8 * d8 < 1.0D) lake[(l * 16 + i1) * 8 + j1] = true;
                    }
                }
            }
        }
        for (int x = 0; x < 16; ++x) {
            for (int z = 0; z < 16; ++z) {
                for (int y = 0; y < 8; ++y) {
                    if (edge(lake, x, z, y)) {
                        BlockPos p = position.add(x, y, z);
                        Material m = material(worldIn, p);
                        if (y >= 4 && m.isLiquid()) return false;
                        if (y < 4 && !m.isSolid() && worldIn.getBlockState(p).getBlock() != block) return false;
                    }
                }
            }
        }
        IBlockState fluid = block.getDefaultState();
        IBlockState air = org.kylepitcock.legacyconnector.v1_12_2.bridge.Vanilla1122.airState();
        for (int x = 0; x < 16; ++x) {
            for (int z = 0; z < 16; ++z) {
                for (int y = 0; y < 8; ++y) {
                    if (lake[(x * 16 + z) * 8 + y]) worldIn.setBlockState(position.add(x, y, z), y >= 4 ? air : fluid, 2);
                }
            }
        }
        for (int x = 0; x < 16; ++x) {
            for (int z = 0; z < 16; ++z) {
                for (int y = 4; y < 8; ++y) {
                    if (!lake[(x * 16 + z) * 8 + y]) continue;
                    BlockPos below = position.add(x, y - 1, z);
                    if (mcc$modernAt(worldIn, below).is(net.minecraft.world.level.block.Blocks.DIRT) && worldIn.canSeeSky(position.add(x, y, z))) {
                        boolean mycelium = mcc$modernAt(worldIn, below).is(net.minecraft.world.level.block.Blocks.MYCELIUM);
                        mcc$setModern(worldIn, below, (mycelium ? net.minecraft.world.level.block.Blocks.MYCELIUM : net.minecraft.world.level.block.Blocks.GRASS_BLOCK)
                                .defaultBlockState());
                    }
                }
            }
        }
        if (fluid != null && fluid.getMaterial() == Material.LAVA) {
            for (int x = 0; x < 16; ++x) {
                for (int z = 0; z < 16; ++z) {
                    for (int y = 0; y < 8; ++y) {
                        if (edge(lake, x, z, y) && (y < 4 || rand.nextInt(2) != 0) && material(worldIn, position.add(x, y, z)).isSolid()) {
                            mcc$setModern(worldIn, position.add(x, y, z), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
                        }
                    }
                }
            }
        }
        return true;
    }

    private static boolean edge(boolean[] lake, int x, int z, int y) {
        return !lake[(x * 16 + z) * 8 + y] && (x < 15 && lake[((x + 1) * 16 + z) * 8 + y] || x > 0 && lake[((x - 1) * 16 + z) * 8 + y]
                || z < 15 && lake[(x * 16 + z + 1) * 8 + y] || z > 0 && lake[(x * 16 + (z - 1)) * 8 + y] || y < 7 && lake[(x * 16 + z) * 8 + y + 1]
                || y > 0 && lake[(x * 16 + z) * 8 + (y - 1)]);
    }
}
