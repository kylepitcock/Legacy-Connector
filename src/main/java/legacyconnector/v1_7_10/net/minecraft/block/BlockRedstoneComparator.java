package legacyconnector.v1_7_10.net.minecraft.block;

/**
 * Generated shell of the 1.7.10 class {@code net.minecraft.block.BlockRedstoneComparator}: correct type hierarchy for {@code instanceof} checks and
 * subclassing, legacy constructors, inherited default behaviour. Vanilla-specific logic is not ported yet.
 * Regenerate with tools/gen_vanilla_shims.py; replace with a hand-written class to add behaviour.
 */
public class BlockRedstoneComparator extends legacyconnector.v1_7_10.net.minecraft.block.BlockRedstoneDiode {
    public BlockRedstoneComparator(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info) {
        super(info);
    }

    protected BlockRedstoneComparator(legacyconnector.v1_7_10.net.minecraft.block.material.Material material) {
        super(material);
    }

    public BlockRedstoneComparator(boolean p0) {
        super(p0);
    }

    /** Whether the comparator is in subtract mode. */
    public boolean func_149969_d(int meta) {
        return (meta & 4) == 4;
    }
}
