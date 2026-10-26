package legacyconnector.v1_7_10.net.minecraft.block;

/**
 * Generated shell of the 1.7.10 class {@code net.minecraft.block.BlockStainedGlassPane}: correct type hierarchy for {@code instanceof} checks and
 * subclassing, legacy constructors, inherited default behaviour. Vanilla-specific logic is not ported yet.
 * Regenerate with tools/gen_vanilla_shims.py; replace with a hand-written class to add behaviour.
 */
public class BlockStainedGlassPane extends legacyconnector.v1_7_10.net.minecraft.block.BlockPane {
    public BlockStainedGlassPane(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info) {
        super(info);
    }

    protected BlockStainedGlassPane(legacyconnector.v1_7_10.net.minecraft.block.material.Material material) {
        super(material);
    }

    public BlockStainedGlassPane() {
        this(legacyconnector.v1_7_10.net.minecraft.block.material.Material.glass);
    }

    private static final String[] mcc$DYES = {"black", "red", "green", "brown", "blue", "purple", "cyan", "silver", "gray", "pink", "lime", "yellow",
            "light_blue", "magenta", "orange", "white"};
    /** The faces (field_150106_a) and edges (field_150105_b) by dye colour. */
    public static final legacyconnector.v1_7_10.net.minecraft.util.IIcon[] field_150106_a = new legacyconnector.v1_7_10.net.minecraft.util.IIcon[16];
    public static final legacyconnector.v1_7_10.net.minecraft.util.IIcon[] field_150105_b = new legacyconnector.v1_7_10.net.minecraft.util.IIcon[16];

    public static int func_150103_c(int meta) {
        return meta & 15;
    }

    @Override
    public void registerBlockIcons(legacyconnector.v1_7_10.net.minecraft.client.renderer.texture.IIconRegister register) {
        super.registerBlockIcons(register);
        for (int i = 0; i < 16; i++) {
            field_150106_a[i] = register.registerIcon(getTextureName() + "_" + mcc$DYES[func_150103_c(i)]);
            field_150105_b[i] = register.registerIcon(getTextureName() + "_pane_top_" + mcc$DYES[func_150103_c(i)]);
        }
    }

    /** The icon of the pane's edges for its metadata (wool colour order). */
    public legacyconnector.v1_7_10.net.minecraft.util.IIcon func_150104_b(int meta) {
        legacyconnector.v1_7_10.net.minecraft.util.IIcon icon = field_150105_b[~meta & 15];
        return icon != null ? icon : mcc$vanillaIcon("glass_pane_top_" + mcc$DYES[~meta & 15]);
    }
}
