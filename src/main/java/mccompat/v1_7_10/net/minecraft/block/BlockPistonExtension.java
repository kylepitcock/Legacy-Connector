package mccompat.v1_7_10.net.minecraft.block;

/**
 * Generated shell of the 1.7.10 class {@code net.minecraft.block.BlockPistonExtension}: correct type hierarchy for {@code instanceof} checks and
 * subclassing, legacy constructors, inherited default behaviour. Vanilla-specific logic is not ported yet.
 * Regenerate with tools/gen_vanilla_shims.py; replace with a hand-written class to add behaviour.
 */
public class BlockPistonExtension extends mccompat.v1_7_10.net.minecraft.block.Block {
    public BlockPistonExtension(org.kylepitcock.mccompat.v1_7_10.bridge.VanillaInfo info) {
        super(info);
    }

    protected BlockPistonExtension(mccompat.v1_7_10.net.minecraft.block.material.Material material) {
        super(material);
    }

    public BlockPistonExtension() {
        this(mccompat.v1_7_10.net.minecraft.block.material.Material.piston);
    }
}
