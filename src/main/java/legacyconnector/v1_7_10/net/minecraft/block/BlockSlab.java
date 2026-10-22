package legacyconnector.v1_7_10.net.minecraft.block;

/**
 * Generated shell of the 1.7.10 class {@code net.minecraft.block.BlockSlab}: correct type hierarchy for {@code instanceof} checks and
 * subclassing, legacy constructors, inherited default behaviour. Vanilla-specific logic is not ported yet.
 * Regenerate with tools/gen_vanilla_shims.py; replace with a hand-written class to add behaviour.
 */
public class BlockSlab extends legacyconnector.v1_7_10.net.minecraft.block.Block {
    /** Whether this is the double slab (mods create the other half when it is false). */
    protected boolean isFullBlock;

    public BlockSlab(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info) {
        super(info);
    }

    protected BlockSlab(legacyconnector.v1_7_10.net.minecraft.block.material.Material material) {
        super(material);
    }

    public BlockSlab(boolean p0, legacyconnector.v1_7_10.net.minecraft.block.material.Material p1) {
        this(p1);
        this.isFullBlock = p0;
    }
}
