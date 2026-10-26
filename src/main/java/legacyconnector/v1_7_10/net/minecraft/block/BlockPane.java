package legacyconnector.v1_7_10.net.minecraft.block;

/**
 * Generated shell of the 1.7.10 class {@code net.minecraft.block.BlockPane}: correct type hierarchy for {@code instanceof} checks and
 * subclassing, legacy constructors, inherited default behaviour. Vanilla-specific logic is not ported yet.
 * Regenerate with tools/gen_vanilla_shims.py; replace with a hand-written class to add behaviour.
 */
public class BlockPane extends legacyconnector.v1_7_10.net.minecraft.block.Block {
    public BlockPane(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info) {
        super(info);
    }

    protected BlockPane(legacyconnector.v1_7_10.net.minecraft.block.material.Material material) {
        super(material);
    }

    protected BlockPane(java.lang.String p0, java.lang.String p1, legacyconnector.v1_7_10.net.minecraft.block.material.Material p2, boolean p3) {
        this(p2);
        this.field_150101_M = p0;
        this.field_150100_a = p1;
        this.field_150099_b = p3;
    }

    public boolean canPaneConnectTo(legacyconnector.v1_7_10.net.minecraft.world.IBlockAccess world, int x, int y, int z,
            legacyconnector.v1_7_10.net.minecraftforge.common.util.ForgeDirection direction) {
        legacyconnector.v1_7_10.net.minecraft.block.Block other = world.getBlock(x, y, z);
        return other == this || other.isOpaqueCube();
    }

    /** The texture of the pane's edges (field_150100_a) and of its faces (field_150101_M), as passed to the constructor. */
    public String field_150100_a, field_150101_M;
    public boolean field_150099_b;
    public legacyconnector.v1_7_10.net.minecraft.util.IIcon field_150102_N;

    @Override
    public void registerBlockIcons(legacyconnector.v1_7_10.net.minecraft.client.renderer.texture.IIconRegister register) {
        blockIcon = register.registerIcon(field_150101_M != null ? field_150101_M : getTextureName());
        field_150102_N = register.registerIcon(field_150100_a != null ? field_150100_a : getTextureName());
    }

    /** The icon of the pane's edges. */
    public legacyconnector.v1_7_10.net.minecraft.util.IIcon func_150097_e() {
        if (field_150102_N != null) return field_150102_N;
        if (mcc$vanilla != null) return mcc$vanillaIcon(mcc$vanilla.legacyName().endsWith("iron_bars") ? "iron_bars" : "glass_pane_top");
        return blockIcon;
    }

    /** Whether a pane connects to a block next to it: full blocks, itself, glass and other panes. */
    public final boolean canPaneConnectToBlock(Block block) {
        return block.isFullBlock() || block == this || block == legacyconnector.v1_7_10.net.minecraft.init.Blocks.glass || block == legacyconnector.v1_7_10.net.minecraft.init.Blocks.stained_glass
                || block == legacyconnector.v1_7_10.net.minecraft.init.Blocks.stained_glass_pane || block instanceof BlockPane;
    }
}
