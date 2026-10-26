package legacyconnector.v1_7_10.net.minecraft.block;

/**
 * Generated shell of the 1.7.10 class {@code net.minecraft.block.BlockCauldron}: correct type hierarchy for {@code instanceof} checks and
 * subclassing, legacy constructors, inherited default behaviour. Vanilla-specific logic is not ported yet.
 * Regenerate with tools/gen_vanilla_shims.py; replace with a hand-written class to add behaviour.
 */
public class BlockCauldron extends legacyconnector.v1_7_10.net.minecraft.block.Block {
    public BlockCauldron(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info) {
        super(info);
    }

    protected BlockCauldron(legacyconnector.v1_7_10.net.minecraft.block.material.Material material) {
        super(material);
    }

    public BlockCauldron() {
        this(legacyconnector.v1_7_10.net.minecraft.block.material.Material.iron);
    }

    public legacyconnector.v1_7_10.net.minecraft.util.IIcon iconInner, iconTop, iconBottom;

    @Override
    public void registerBlockIcons(legacyconnector.v1_7_10.net.minecraft.client.renderer.texture.IIconRegister register) {
        iconInner = register.registerIcon(getTextureName() + "_inner");
        iconTop = register.registerIcon(getTextureName() + "_top");
        iconBottom = register.registerIcon(getTextureName() + "_bottom");
        blockIcon = register.registerIcon(getTextureName() + "_side");
    }

    /** The height of the water surface for a fill level (0-3). */
    public static float getRenderLiquidLevel(int level) {
        int l = Math.max(0, Math.min(level, 3));
        return (6 + 3 * l) / 16.0F;
    }

    public static int getPowerFromMeta(int meta) {
        return meta;
    }

    /** The cauldron's "inner" and "bottom" textures. */
    public static legacyconnector.v1_7_10.net.minecraft.util.IIcon getCauldronIcon(String name) {
        BlockCauldron cauldron = legacyconnector.v1_7_10.net.minecraft.init.Blocks.cauldron;
        legacyconnector.v1_7_10.net.minecraft.util.IIcon icon = name.equals("inner") ? cauldron.iconInner : name.equals("bottom") ? cauldron.iconBottom : null;
        if (icon != null) return icon;
        return name.equals("inner") || name.equals("bottom") ? mcc$vanillaIcon("cauldron_" + name) : null;
    }
}
