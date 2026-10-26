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

    public legacyconnector.v1_7_10.net.minecraft.util.IIcon hopperOutsideIcon, hopperTopIcon, hopperInsideIcon;

    @Override
    public void registerBlockIcons(legacyconnector.v1_7_10.net.minecraft.client.renderer.texture.IIconRegister register) {
        hopperOutsideIcon = register.registerIcon("hopper_outside");
        hopperTopIcon = register.registerIcon("hopper_top");
        hopperInsideIcon = register.registerIcon("hopper_inside");
    }

    /** The output direction (0-5) in the metadata. */
    public static int getDirectionFromMetadata(int meta) {
        return meta & 7;
    }

    /** Whether the hopper is enabled (not locked by redstone). */
    public static boolean getActiveStateFromMetadata(int meta) {
        return (meta & 8) != 8;
    }

    /** The vanilla hopper's "hopper_outside" and "hopper_inside" textures. */
    public static legacyconnector.v1_7_10.net.minecraft.util.IIcon getHopperIcon(String name) {
        BlockHopper hopper = legacyconnector.v1_7_10.net.minecraft.init.Blocks.hopper;
        legacyconnector.v1_7_10.net.minecraft.util.IIcon icon = name.equals("hopper_outside") ? hopper.hopperOutsideIcon
                : name.equals("hopper_inside") ? hopper.hopperInsideIcon : null;
        if (icon != null) return icon;
        return name.equals("hopper_outside") || name.equals("hopper_inside") ? mcc$vanillaIcon(name) : null;
    }
}
