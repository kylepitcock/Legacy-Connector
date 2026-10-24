package legacyconnector.v1_7_10.net.minecraft.block;

/**
 * Generated shell of the 1.7.10 class {@code net.minecraft.block.BlockPistonBase}: correct type hierarchy for {@code instanceof} checks and
 * subclassing, legacy constructors, inherited default behaviour. Vanilla-specific logic is not ported yet.
 * Regenerate with tools/gen_vanilla_shims.py; replace with a hand-written class to add behaviour.
 */
public class BlockPistonBase extends legacyconnector.v1_7_10.net.minecraft.block.Block {
    /** Whether this is the sticky piston. */
    protected boolean isSticky;

    public BlockPistonBase(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info) {
        super(info);
    }

    protected BlockPistonBase(legacyconnector.v1_7_10.net.minecraft.block.material.Material material) {
        super(material);
    }

    public BlockPistonBase(boolean p0) {
        this(legacyconnector.v1_7_10.net.minecraft.block.material.Material.piston);
        this.isSticky = p0;
    }

    /** func_150079_i: whether the piston can push the blocks in front of it (the push logic is the engine's). */
    public boolean tryExtend(legacyconnector.v1_7_10.net.minecraft.world.World world, int x, int y, int z, int meta) {
        return false;
    }

    public static int determineOrientation(legacyconnector.v1_7_10.net.minecraft.world.World world, int x, int y, int z,
            legacyconnector.v1_7_10.net.minecraft.entity.EntityLivingBase entity) {
        if (Math.abs((float) entity.posX - x) < 2.0F && Math.abs((float) entity.posZ - z) < 2.0F) {
            double eye = entity.posY + 1.82D - entity.yOffset;
            if (eye - y > 2.0D) return 1;
            if (y - eye > 0.0D) return 0;
        }
        int facing = (int) Math.floor(entity.rotationYaw * 4.0F / 360.0F + 0.5D) & 3;
        return facing == 0 ? 2 : facing == 1 ? 5 : facing == 2 ? 3 : facing == 3 ? 4 : 0;
    }
}
