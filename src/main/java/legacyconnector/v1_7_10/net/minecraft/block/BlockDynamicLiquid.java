package legacyconnector.v1_7_10.net.minecraft.block;

import java.util.Random;
import legacyconnector.v1_7_10.net.minecraft.block.material.Material;
import legacyconnector.v1_7_10.net.minecraft.init.Blocks;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/**
 * Flowing water or lava, ported from 1.7.10: on its scheduled ticks it takes the level of its neighbours (two sources make a
 * new water source), falls, and spreads towards the nearest drop; when it settles it turns into its still block (the block
 * with the next id, as in 1.7.10).
 */
public class BlockDynamicLiquid extends BlockLiquid {
    /** Sources next to the block found by the last tick. */
    int field_149815_a;
    /** Directions (-x, +x, -z, +z) the liquid spreads in. */
    boolean[] field_149814_b = new boolean[4];
    /** Flow cost per direction. */
    int[] field_149816_M = new int[4];

    public BlockDynamicLiquid(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info) {
        super(info);
    }

    protected BlockDynamicLiquid(Material material) {
        super(material);
    }

    /** Turns this flowing block into the still one (the block with the next id, when it is a still liquid of this material). */
    private void func_149811_n(World world, int x, int y, int z) {
        Block still = Block.getBlockById(Block.getIdFromBlock(this) + 1);
        if (!(still instanceof BlockStaticLiquid) || still.getMaterial() != blockMaterial) return;
        world.setBlock(x, y, z, still, world.getBlockMetadata(x, y, z), 2);
    }

    @Override
    public void updateTick(World world, int x, int y, int z, Random random) {
        int level = func_149804_e(world, x, y, z);
        int step = blockMaterial == Material.lava && !World.mcc$get$provider(world).isHellWorld ? 2 : 1;
        int delay = tickRate(world);
        if (level > 0) {
            field_149815_a = 0;
            int smallest = func_149810_a(world, x - 1, y, z, -100);
            smallest = func_149810_a(world, x + 1, y, z, smallest);
            smallest = func_149810_a(world, x, y, z - 1, smallest);
            smallest = func_149810_a(world, x, y, z + 1, smallest);
            int next = smallest + step;
            if (next >= 8 || smallest < 0) next = -1;
            int above = func_149804_e(world, x, y + 1, z);
            if (above >= 0) next = above >= 8 ? above : above + 8;
            if (field_149815_a >= 2 && blockMaterial == Material.water) {
                Block below = world.getBlock(x, y - 1, z);
                if (below.getMaterial().isSolid() || below.getMaterial() == blockMaterial && world.getBlockMetadata(x, y - 1, z) == 0) next = 0;
            }
            if (blockMaterial == Material.lava && level < 8 && next < 8 && next > level && random.nextInt(4) != 0) delay *= 4;
            if (next == level) {
                func_149811_n(world, x, y, z);
            } else {
                level = next;
                if (next < 0) {
                    world.setBlockToAir(x, y, z);
                } else {
                    world.setBlockMetadataWithNotify(x, y, z, next, 2);
                    world.scheduleBlockUpdate(x, y, z, this, delay);
                    world.notifyBlocksOfNeighborChange(x, y, z, this);
                }
            }
        } else {
            func_149811_n(world, x, y, z);
        }
        if (func_149809_q(world, x, y - 1, z)) {
            if (blockMaterial == Material.lava && world.getBlock(x, y - 1, z).getMaterial() == Material.water) {
                world.setBlock(x, y - 1, z, Blocks.stone);
                func_149799_m(world, x, y - 1, z);
                return;
            }
            func_149813_h(world, x, y - 1, z, level >= 8 ? level : level + 8);
        } else if (level >= 0 && (level == 0 || func_149807_p(world, x, y - 1, z))) {
            boolean[] directions = func_149808_o(world, x, y, z);
            int spread = level >= 8 ? 1 : level + step;
            if (spread >= 8) return;
            if (directions[0]) func_149813_h(world, x - 1, y, z, spread);
            if (directions[1]) func_149813_h(world, x + 1, y, z, spread);
            if (directions[2]) func_149813_h(world, x, y, z - 1, spread);
            if (directions[3]) func_149813_h(world, x, y, z + 1, spread);
        }
    }

    /** Flows into a position with a level, washing away (or, for lava, burning) what was there. */
    private void func_149813_h(World world, int x, int y, int z, int meta) {
        if (!func_149809_q(world, x, y, z)) return;
        Block block = world.getBlock(x, y, z);
        if (blockMaterial == Material.lava) func_149799_m(world, x, y, z);
        else block.dropBlockAsItem(world, x, y, z, world.getBlockMetadata(x, y, z), 0);
        world.setBlock(x, y, z, this, meta, 3);
    }

    /** The distance (up to 4) to the nearest drop reachable from a position, not going back the way it came. */
    private int func_149812_c(World world, int x, int y, int z, int distance, int from) {
        int best = 1000;
        for (int side = 0; side < 4; ++side) {
            if (side == 0 && from == 1 || side == 1 && from == 0 || side == 2 && from == 3 || side == 3 && from == 2) continue;
            int nx = x + (side == 0 ? -1 : side == 1 ? 1 : 0);
            int nz = z + (side == 2 ? -1 : side == 3 ? 1 : 0);
            if (func_149807_p(world, nx, y, nz) || world.getBlock(nx, y, nz).getMaterial() == blockMaterial && world.getBlockMetadata(nx, y, nz) == 0) continue;
            if (!func_149807_p(world, nx, y - 1, nz)) return distance;
            if (distance < 4) {
                int cost = func_149812_c(world, nx, y, nz, distance + 1, side);
                if (cost < best) best = cost;
            }
        }
        return best;
    }

    /** The directions with the cheapest way down. */
    private boolean[] func_149808_o(World world, int x, int y, int z) {
        for (int side = 0; side < 4; ++side) {
            field_149816_M[side] = 1000;
            int nx = x + (side == 0 ? -1 : side == 1 ? 1 : 0);
            int nz = z + (side == 2 ? -1 : side == 3 ? 1 : 0);
            if (func_149807_p(world, nx, y, nz) || world.getBlock(nx, y, nz).getMaterial() == blockMaterial && world.getBlockMetadata(nx, y, nz) == 0) continue;
            field_149816_M[side] = func_149807_p(world, nx, y - 1, nz) ? func_149812_c(world, nx, y, nz, 1, side) : 0;
        }
        int min = field_149816_M[0];
        for (int side = 1; side < 4; ++side) if (field_149816_M[side] < min) min = field_149816_M[side];
        for (int side = 0; side < 4; ++side) field_149814_b[side] = field_149816_M[side] == min;
        return field_149814_b;
    }

    /** Whether the liquid cannot flow into the block at the position (doors, signs, ladders, cane, solid blocks, portals). */
    private boolean func_149807_p(World world, int x, int y, int z) {
        Block block = world.getBlock(x, y, z);
        if (block == Blocks.wooden_door || block == Blocks.iron_door || block == Blocks.standing_sign || block == Blocks.ladder || block == Blocks.reeds) return true;
        Material material = block.getMaterial();
        return material == Material.portal || material.blocksMovement();
    }

    /** {@code getSmallestFlowDecay}: the lower of {@code current} and the level at the position (counting sources). */
    protected int func_149810_a(World world, int x, int y, int z, int current) {
        int level = func_149804_e(world, x, y, z);
        if (level < 0) return current;
        if (level == 0) ++field_149815_a;
        if (level >= 8) level = 0;
        return current >= 0 && level >= current ? current : level;
    }

    /** Whether the liquid can wash away the block at the position. */
    private boolean func_149809_q(World world, int x, int y, int z) {
        Material material = world.getBlock(x, y, z).getMaterial();
        return material != blockMaterial && material != Material.lava && !func_149807_p(world, x, y, z);
    }

    @Override
    public void onBlockAdded(World world, int x, int y, int z) {
        super.onBlockAdded(world, x, y, z);
        if (world.getBlock(x, y, z) == this) world.scheduleBlockUpdate(x, y, z, this, tickRate(world));
    }

    public boolean requiresUpdates() {
        return true;
    }
}
