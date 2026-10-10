package mccompat.v1_7_10.net.minecraft.block;

/**
 * Generated shell of the 1.7.10 class {@code net.minecraft.block.BlockRedstoneRepeater}: correct type hierarchy for {@code instanceof} checks and
 * subclassing, legacy constructors, inherited default behaviour. Vanilla-specific logic is not ported yet.
 * Regenerate with tools/gen_vanilla_shims.py; replace with a hand-written class to add behaviour.
 */
public class BlockRedstoneRepeater extends mccompat.v1_7_10.net.minecraft.block.BlockRedstoneDiode {
    public BlockRedstoneRepeater(org.kylepitcock.mccompat.v1_7_10.bridge.VanillaInfo info) {
        super(info);
    }

    protected BlockRedstoneRepeater(mccompat.v1_7_10.net.minecraft.block.material.Material material) {
        super(material);
    }

    protected BlockRedstoneRepeater(boolean p0) {
        this(mccompat.v1_7_10.net.minecraft.block.material.Material.circuits);
    }
}
