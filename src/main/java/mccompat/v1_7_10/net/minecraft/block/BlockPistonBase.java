package mccompat.v1_7_10.net.minecraft.block;

/**
 * Generated shell of the 1.7.10 class {@code net.minecraft.block.BlockPistonBase}: correct type hierarchy for {@code instanceof} checks and
 * subclassing, legacy constructors, inherited default behaviour. Vanilla-specific logic is not ported yet.
 * Regenerate with tools/gen_vanilla_shims.py; replace with a hand-written class to add behaviour.
 */
public class BlockPistonBase extends mccompat.v1_7_10.net.minecraft.block.Block {
    /** Whether this is the sticky piston. */
    protected boolean isSticky;

    public BlockPistonBase(org.kylepitcock.mccompat.v1_7_10.bridge.VanillaInfo info) {
        super(info);
    }

    protected BlockPistonBase(mccompat.v1_7_10.net.minecraft.block.material.Material material) {
        super(material);
    }

    public BlockPistonBase(boolean p0) {
        this(mccompat.v1_7_10.net.minecraft.block.material.Material.piston);
        this.isSticky = p0;
    }

    /** func_150079_i: whether the piston can push the blocks in front of it (the push logic is the engine's). */
    public boolean tryExtend(mccompat.v1_7_10.net.minecraft.world.World world, int x, int y, int z, int meta) {
        return false;
    }
}
