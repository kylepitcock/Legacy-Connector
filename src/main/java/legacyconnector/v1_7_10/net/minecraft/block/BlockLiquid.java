package legacyconnector.v1_7_10.net.minecraft.block;

/**
 * Generated shell of the 1.7.10 class {@code net.minecraft.block.BlockLiquid}: correct type hierarchy for {@code instanceof} checks and
 * subclassing, legacy constructors, inherited default behaviour. Vanilla-specific logic is not ported yet.
 * Regenerate with tools/gen_vanilla_shims.py; replace with a hand-written class to add behaviour.
 */
public class BlockLiquid extends legacyconnector.v1_7_10.net.minecraft.block.Block {
    public BlockLiquid(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info) {
        super(info);
    }

    protected BlockLiquid(legacyconnector.v1_7_10.net.minecraft.block.material.Material material) {
        super(material);
    }

    public static float getLiquidHeightPercent(int meta) {
        if (meta >= 8) meta = 0;
        return (meta + 1) / 9.0F;
    }
}
