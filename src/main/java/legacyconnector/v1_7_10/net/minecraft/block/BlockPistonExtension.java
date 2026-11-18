package legacyconnector.v1_7_10.net.minecraft.block;

/**
 * Generated shell of the 1.7.10 class {@code net.minecraft.block.BlockPistonExtension}: correct type hierarchy for {@code instanceof} checks and
 * subclassing, legacy constructors, inherited default behaviour. Vanilla-specific logic is not ported yet.
 * Regenerate with tools/gen_vanilla_shims.py; replace with a hand-written class to add behaviour.
 */
public class BlockPistonExtension extends legacyconnector.v1_7_10.net.minecraft.block.Block {
    public BlockPistonExtension(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info) {
        super(info);
    }

    protected BlockPistonExtension(legacyconnector.v1_7_10.net.minecraft.block.material.Material material) {
        super(material);
    }

    public BlockPistonExtension() {
        this(legacyconnector.v1_7_10.net.minecraft.block.material.Material.piston);
    }

    /** The facing (0-5) of a piston head's metadata (bit 3 marks a sticky head). */
    public static int getDirectionMeta(int meta) {
        return meta & 7;
    }

    private legacyconnector.v1_7_10.net.minecraft.util.IIcon mcc$headTexture;

    /** {@code func_150086_a} (1.6.4: {@code setHeadTexture}): the icon of the head's face while a piston with a mod's head extends, until {@link #clearHeadTexture()}. */
    public void func_150086_a(legacyconnector.v1_7_10.net.minecraft.util.IIcon icon) {
        this.mcc$headTexture = icon;
    }

    /** Goes back to the vanilla head icons. */
    public void clearHeadTexture() {
        this.mcc$headTexture = null;
    }

    @Override
    public legacyconnector.v1_7_10.net.minecraft.util.IIcon getIcon(int side, int meta) {
        legacyconnector.v1_7_10.net.minecraft.util.IIcon override = mcc$headTexture;
        if (override != null && side == getDirectionMeta(meta)) return override;
        return super.getIcon(side, meta);
    }
}
