package legacyconnector.v1_7_10.net.minecraft.block;

import legacyconnector.v1_7_10.net.minecraft.world.World;

/**
 * Powered (and activator) rails: bit 3 is set while the rail is powered, directly or through up to eight connected rails of
 * the same kind.
 */
public class BlockRailPowered extends BlockRailBase {
    public BlockRailPowered(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info) {
        super(info);
    }

    protected BlockRailPowered() {
        super(true);
    }

    /** Whether power reaches this rail along the track in one direction ({@code forward}), up to 8 rails away. */
    protected boolean func_150058_a(World world, int x, int y, int z, int meta, boolean forward, int depth) {
        if (depth >= 8) return false;
        int shape = meta & 7;
        boolean below = true;
        switch (shape) {
            case 0 -> {
                if (forward) ++z;
                else --z;
            }
            case 1 -> {
                if (forward) --x;
                else ++x;
            }
            case 2 -> {
                if (forward) {
                    --x;
                } else {
                    ++x;
                    ++y;
                    below = false;
                }
                shape = 1;
            }
            case 3 -> {
                if (forward) {
                    --x;
                    ++y;
                    below = false;
                } else {
                    ++x;
                }
                shape = 1;
            }
            case 4 -> {
                if (forward) {
                    ++z;
                } else {
                    --z;
                    ++y;
                    below = false;
                }
                shape = 0;
            }
            case 5 -> {
                if (forward) {
                    ++z;
                    ++y;
                    below = false;
                } else {
                    --z;
                }
                shape = 0;
            }
            default -> { }
        }
        return func_150057_a(world, x, y, z, forward, depth, shape) || below && func_150057_a(world, x, y - 1, z, forward, depth, shape);
    }

    /** Whether the rail at the position is this kind, lines up with {@code shape} and is powered. */
    protected boolean func_150057_a(World world, int x, int y, int z, boolean forward, int depth, int shape) {
        if (world.getBlock(x, y, z) != this) return false;
        int meta = world.getBlockMetadata(x, y, z);
        int other = meta & 7;
        if (shape == 1 && (other == 0 || other == 4 || other == 5)) return false;
        if (shape == 0 && (other == 1 || other == 2 || other == 3)) return false;
        if ((meta & 8) == 0) return false;
        return world.isBlockIndirectlyGettingPowered(x, y, z) || func_150058_a(world, x, y, z, meta, forward, depth + 1);
    }

    @Override
    protected void onRedstoneSignal(World world, int x, int y, int z, int meta, int shape, Block neighbor) {
        boolean powered = world.isBlockIndirectlyGettingPowered(x, y, z) || func_150058_a(world, x, y, z, meta, true, 0)
                || func_150058_a(world, x, y, z, meta, false, 0);
        boolean changed = false;
        if (powered && (meta & 8) == 0) {
            world.setBlockMetadataWithNotify(x, y, z, shape | 8, 3);
            changed = true;
        } else if (!powered && (meta & 8) != 0) {
            world.setBlockMetadataWithNotify(x, y, z, shape, 3);
            changed = true;
        }
        if (changed) {
            world.notifyBlocksOfNeighborChange(x, y - 1, z, this);
            if (shape == 2 || shape == 3 || shape == 4 || shape == 5) world.notifyBlocksOfNeighborChange(x, y + 1, z, this);
        }
    }
}
