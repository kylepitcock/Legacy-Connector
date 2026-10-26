package legacyconnector.v1_7_10.net.minecraft.block;

/**
 * Generated shell of the 1.7.10 class {@code net.minecraft.block.BlockHopper}: correct type hierarchy for {@code instanceof} checks and
 * subclassing, legacy constructors, inherited default behaviour. Vanilla-specific logic is not ported yet.
 * Regenerate with tools/gen_vanilla_shims.py; replace with a hand-written class to add behaviour.
 */
public class BlockHopper extends legacyconnector.v1_7_10.net.minecraft.block.BlockContainer {
    public BlockHopper(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info) {
        super(info);
    }

    protected BlockHopper(legacyconnector.v1_7_10.net.minecraft.block.material.Material material) {
        super(material);
    }

    public BlockHopper() {
        this(legacyconnector.v1_7_10.net.minecraft.block.material.Material.iron);
    }

    /** The facing of a hopper's output (0 down, 2-5 sides) from its metadata. */
    public static int getDirectionFromMetadata(int meta) {
        return meta & 7;
    }

    /** Whether the hopper is enabled (bit 3 is set while it is powered and locked). */
    public static boolean getActiveStateFromMetadata(int meta) {
        return (meta & 8) != 8;
    }

    /** The vanilla hopper textures by their 1.7.10 names: "hopper_outside", "hopper_inside" (null for others). */
    public static legacyconnector.v1_7_10.net.minecraft.util.IIcon getHopperIcon(String name) {
        if ("hopper_outside".equals(name)) return mcc$vanillaIcon("hopper_outside");
        if ("hopper_inside".equals(name)) return mcc$vanillaIcon("hopper_inside");
        return null;
    }

    /** The hopper tile entity at the position, or null. */
    public static legacyconnector.v1_7_10.net.minecraft.tileentity.TileEntityHopper func_149920_e(legacyconnector.v1_7_10.net.minecraft.world.IBlockAccess world,
            int x, int y, int z) {
        return world.getTileEntity(x, y, z) instanceof legacyconnector.v1_7_10.net.minecraft.tileentity.TileEntityHopper h ? h : null;
    }
}
