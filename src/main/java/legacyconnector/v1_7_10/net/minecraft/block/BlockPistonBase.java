package legacyconnector.v1_7_10.net.minecraft.block;

/**
 * Generated shell of the 1.7.10 class {@code net.minecraft.block.BlockPistonBase}: correct type hierarchy for {@code instanceof} checks and
 * subclassing, legacy constructors, inherited default behaviour. Vanilla-specific logic is not ported yet.
 * Regenerate with tools/gen_vanilla_shims.py; replace with a hand-written class to add behaviour.
 */
public class BlockPistonBase extends legacyconnector.v1_7_10.net.minecraft.block.Block {
    /** Whether this is the sticky piston. */
    protected boolean isSticky;

    public BlockPistonBase(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info) {
        super(info);
    }

    protected BlockPistonBase(legacyconnector.v1_7_10.net.minecraft.block.material.Material material) {
        super(material);
    }

    public BlockPistonBase(boolean p0) {
        this(legacyconnector.v1_7_10.net.minecraft.block.material.Material.piston);
        this.isSticky = p0;
    }

    /** func_150079_i: whether the piston can push the blocks in front of it (the push logic is the engine's). */
    public boolean tryExtend(legacyconnector.v1_7_10.net.minecraft.world.World world, int x, int y, int z, int meta) {
        return false;
    }

    /** The facing (0-5) of a piston's metadata. */
    public static int getPistonOrientation(int meta) {
        return meta & 7;
    }

    /** Whether a piston's metadata says it is extended. */
    public static boolean isExtended(int meta) {
        return (meta & 8) != 0;
    }

    public static int determineOrientation(legacyconnector.v1_7_10.net.minecraft.world.World world, int x, int y, int z,
            legacyconnector.v1_7_10.net.minecraft.entity.EntityLivingBase entity) {
        if (Math.abs((float) legacyconnector.v1_7_10.net.minecraft.entity.Entity.mcc$get$posX(entity) - x) < 2.0F && Math.abs((float) legacyconnector.v1_7_10.net.minecraft.entity.Entity.mcc$get$posZ(entity) - z) < 2.0F) {
            double eye = legacyconnector.v1_7_10.net.minecraft.entity.Entity.mcc$get$posY(entity) + 1.82D - entity.yOffset;
            if (eye - y > 2.0D) return 1;
            if (y - eye > 0.0D) return 0;
        }
        int facing = (int) Math.floor(legacyconnector.v1_7_10.net.minecraft.entity.Entity.mcc$get$rotationYaw(entity) * 4.0F / 360.0F + 0.5D) & 3;
        return facing == 0 ? 2 : facing == 1 ? 5 : facing == 2 ? 3 : facing == 3 ? 4 : 0;
    }

    public legacyconnector.v1_7_10.net.minecraft.util.IIcon topIcon, bottomIcon, innerTopIcon;

    @Override
    public void registerBlockIcons(legacyconnector.v1_7_10.net.minecraft.client.renderer.texture.IIconRegister register) {
        blockIcon = register.registerIcon("piston_side");
        topIcon = register.registerIcon(isSticky ? "piston_top_sticky" : "piston_top_normal");
        innerTopIcon = register.registerIcon("piston_inner");
        bottomIcon = register.registerIcon("piston_bottom");
    }

    /** The piston textures by name ("piston_side", "piston_top_normal", "piston_top_sticky", "piston_inner"). */
    public static legacyconnector.v1_7_10.net.minecraft.util.IIcon getPistonBaseIcon(String name) {
        legacyconnector.v1_7_10.net.minecraft.util.IIcon icon = switch (name) {
            case "piston_side" -> legacyconnector.v1_7_10.net.minecraft.init.Blocks.piston.blockIcon;
            case "piston_top_normal" -> legacyconnector.v1_7_10.net.minecraft.init.Blocks.piston.topIcon;
            case "piston_top_sticky" -> legacyconnector.v1_7_10.net.minecraft.init.Blocks.sticky_piston.topIcon;
            case "piston_inner" -> legacyconnector.v1_7_10.net.minecraft.init.Blocks.piston.innerTopIcon;
            default -> null;
        };
        if (icon != null) return icon;
        return switch (name) {
            case "piston_side", "piston_top_normal", "piston_top_sticky", "piston_inner" -> mcc$vanillaIcon(name);
            default -> null;
        };
    }

    /** The icon of the piston head's face. */
    public legacyconnector.v1_7_10.net.minecraft.util.IIcon getPistonExtensionTexture() {
        return topIcon != null ? topIcon : mcc$vanillaIcon(isSticky ? "piston_top_sticky" : "piston_top_normal");
    }

    public void func_150070_b(float x1, float y1, float z1, float x2, float y2, float z2) {
        setBlockBounds(x1, y1, z1, x2, y2, z2);
    }
}
