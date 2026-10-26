package legacyconnector.v1_7_10.net.minecraft.block;

/**
 * Generated shell of the 1.7.10 class {@code net.minecraft.block.BlockRedstoneWire}: correct type hierarchy for {@code instanceof} checks and
 * subclassing, legacy constructors, inherited default behaviour. Vanilla-specific logic is not ported yet.
 * Regenerate with tools/gen_vanilla_shims.py; replace with a hand-written class to add behaviour.
 */
public class BlockRedstoneWire extends legacyconnector.v1_7_10.net.minecraft.block.Block {
    public BlockRedstoneWire(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info) {
        super(info);
    }

    protected BlockRedstoneWire(legacyconnector.v1_7_10.net.minecraft.block.material.Material material) {
        super(material);
    }

    public BlockRedstoneWire() {
        this(legacyconnector.v1_7_10.net.minecraft.block.material.Material.circuits);
    }

    public legacyconnector.v1_7_10.net.minecraft.util.IIcon redstoneCrossIcon, redstoneLineIcon, redstoneCrossOverlayIcon, redstoneLineOverlayIcon;

    @Override
    public void registerBlockIcons(legacyconnector.v1_7_10.net.minecraft.client.renderer.texture.IIconRegister register) {
        redstoneCrossIcon = register.registerIcon(getTextureName() + "_cross");
        redstoneLineIcon = register.registerIcon(getTextureName() + "_line");
        redstoneCrossOverlayIcon = register.registerIcon(getTextureName() + "_cross_overlay");
        redstoneLineOverlayIcon = register.registerIcon(getTextureName() + "_line_overlay");
        blockIcon = redstoneCrossIcon;
    }

    /** The wire's icons by part ("cross", "line", "cross_overlay", "line_overlay"), from the vanilla wire. */
    public static legacyconnector.v1_7_10.net.minecraft.util.IIcon getRedstoneWireIcon(String part) {
        BlockRedstoneWire wire = legacyconnector.v1_7_10.net.minecraft.init.Blocks.redstone_wire;
        legacyconnector.v1_7_10.net.minecraft.util.IIcon icon = switch (part) {
            case "cross" -> wire.redstoneCrossIcon;
            case "line" -> wire.redstoneLineIcon;
            case "cross_overlay" -> wire.redstoneCrossOverlayIcon;
            case "line_overlay" -> wire.redstoneLineOverlayIcon;
            default -> null;
        };
        if (icon != null) return icon;
        return switch (part) {
            case "cross", "line", "cross_overlay", "line_overlay" -> mcc$vanillaIcon("redstone_dust_" + part);
            default -> null;
        };
    }

    /** Whether the wire at a neighbour position connects to it: wire, a diode facing the wire, or any power source. */
    public static boolean isPowerProviderOrWire(legacyconnector.v1_7_10.net.minecraft.world.IBlockAccess world, int x, int y, int z, int direction) {
        Block block = world.getBlock(x, y, z);
        if (block == legacyconnector.v1_7_10.net.minecraft.init.Blocks.redstone_wire) return true;
        if (block == legacyconnector.v1_7_10.net.minecraft.init.Blocks.unpowered_repeater || block == legacyconnector.v1_7_10.net.minecraft.init.Blocks.powered_repeater) {
            int meta = world.getBlockMetadata(x, y, z);
            return direction == (meta & 3) || direction == legacyconnector.v1_7_10.net.minecraft.util.Direction.rotateOpposite[meta & 3];
        }
        return block.canProvidePower() && direction != -1;
    }
}
