package legacyconnector.v1_7_10.net.minecraft.block;

/**
 * Generated shell of the 1.7.10 class {@code net.minecraft.block.BlockPistonMoving}: correct type hierarchy for {@code instanceof} checks and
 * subclassing, legacy constructors, inherited default behaviour. Vanilla-specific logic is not ported yet.
 * Regenerate with tools/gen_vanilla_shims.py; replace with a hand-written class to add behaviour.
 */
public class BlockPistonMoving extends legacyconnector.v1_7_10.net.minecraft.block.BlockContainer {
    public BlockPistonMoving(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info) {
        super(info);
    }

    protected BlockPistonMoving(legacyconnector.v1_7_10.net.minecraft.block.material.Material material) {
        super(material);
    }

    public BlockPistonMoving() {
        this(legacyconnector.v1_7_10.net.minecraft.block.material.Material.piston);
    }

    /** The moving-piston tile entity carrying {@code block} with {@code meta} towards {@code orientation}. */
    public static legacyconnector.v1_7_10.net.minecraft.tileentity.TileEntity getTileEntity(Block block, int meta, int orientation, boolean extending,
            boolean renderHead) {
        return new legacyconnector.v1_7_10.net.minecraft.tileentity.TileEntityPiston(block, meta, orientation, extending, renderHead);
    }
}
