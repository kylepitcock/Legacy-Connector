package legacyconnector.v1_7_10.net.minecraft.block;

/**
 * Generated shell of the 1.7.10 class {@code net.minecraft.block.BlockRedstoneRepeater}: correct type hierarchy for {@code instanceof} checks and
 * subclassing, legacy constructors, inherited default behaviour. Vanilla-specific logic is not ported yet.
 * Regenerate with tools/gen_vanilla_shims.py; replace with a hand-written class to add behaviour.
 */
public class BlockRedstoneRepeater extends legacyconnector.v1_7_10.net.minecraft.block.BlockRedstoneDiode {
    public BlockRedstoneRepeater(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info) {
        super(info);
    }

    protected BlockRedstoneRepeater(legacyconnector.v1_7_10.net.minecraft.block.material.Material material) {
        super(material);
    }

    protected BlockRedstoneRepeater(boolean p0) {
        super(p0);
    }

    /** How far the movable torch sits from the centre for each delay setting. */
    public static final double[] repeaterTorchOffset = {-0.0625D, 0.0625D, 0.1875D, 0.3125D};
    public static final int[] repeaterState = {1, 2, 3, 4};

    /** Whether a powered repeater or comparator facing into the side locks this repeater. */
    public boolean func_149910_g(legacyconnector.v1_7_10.net.minecraft.world.IBlockAccess world, int x, int y, int z, int meta) {
        int dir = getDirection(meta);
        return switch (dir) {
            case 0, 2 -> mcc$sidePower(world, x - 1, y, z, 4) > 0 || mcc$sidePower(world, x + 1, y, z, 5) > 0;
            case 1, 3 -> mcc$sidePower(world, x, y, z + 1, 3) > 0 || mcc$sidePower(world, x, y, z - 1, 2) > 0;
            default -> false;
        };
    }

    private static int mcc$sidePower(legacyconnector.v1_7_10.net.minecraft.world.IBlockAccess world, int x, int y, int z, int side) {
        Block block = world.getBlock(x, y, z);
        boolean diode = block == legacyconnector.v1_7_10.net.minecraft.init.Blocks.unpowered_repeater || block == legacyconnector.v1_7_10.net.minecraft.init.Blocks.powered_repeater
                || block == legacyconnector.v1_7_10.net.minecraft.init.Blocks.unpowered_comparator || block == legacyconnector.v1_7_10.net.minecraft.init.Blocks.powered_comparator;
        return diode ? world.isBlockProvidingPowerTo(x, y, z, side) : 0;
    }
}
