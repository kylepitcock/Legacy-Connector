package legacyconnector.v1_7_10.net.minecraft.block;

/**
 * Generated shell of the 1.7.10 class {@code net.minecraft.block.BlockBeacon}: correct type hierarchy for {@code instanceof} checks and
 * subclassing, legacy constructors, inherited default behaviour. Vanilla-specific logic is not ported yet.
 * Regenerate with tools/gen_vanilla_shims.py; replace with a hand-written class to add behaviour.
 */
public class BlockBeacon extends legacyconnector.v1_7_10.net.minecraft.block.BlockContainer {
    public BlockBeacon(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info) {
        super(info);
    }

    protected BlockBeacon(legacyconnector.v1_7_10.net.minecraft.block.material.Material material) {
        super(material);
    }

    public BlockBeacon() {
        this(legacyconnector.v1_7_10.net.minecraft.block.material.Material.glass);
    }
}
