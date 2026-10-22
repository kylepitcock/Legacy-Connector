package legacyconnector.v1_7_10.net.minecraft.block;

/**
 * Generated shell of the 1.7.10 class {@code net.minecraft.block.BlockRedstoneDiode}: correct type hierarchy for {@code instanceof} checks and
 * subclassing, legacy constructors, inherited default behaviour. Vanilla-specific logic is not ported yet.
 * Regenerate with tools/gen_vanilla_shims.py; replace with a hand-written class to add behaviour.
 */
public class BlockRedstoneDiode extends legacyconnector.v1_7_10.net.minecraft.block.BlockDirectional {
    /** Whether this is the powered variant. */
    protected boolean isRepeaterPowered;

    public BlockRedstoneDiode(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info) {
        super(info);
    }

    protected BlockRedstoneDiode(legacyconnector.v1_7_10.net.minecraft.block.material.Material material) {
        super(material);
    }

    protected BlockRedstoneDiode(boolean p0) {
        this(legacyconnector.v1_7_10.net.minecraft.block.material.Material.circuits);
        this.isRepeaterPowered = p0;
    }
}
