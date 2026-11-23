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

    /**
     * {@code func_149964_a}: the collision box of {@code block} (the block being pushed) at {@code x, y, z}, shortened by the push {@code progress}
     * (0 to 1) along {@code direction}; null for air or for another moving piston.
     */
    public legacyconnector.v1_7_10.net.minecraft.util.AxisAlignedBB func_149964_a(legacyconnector.v1_7_10.net.minecraft.world.World world, int x, int y, int z, Block block,
            float progress, int direction) {
        if (block == null || block == this || block == legacyconnector.v1_7_10.net.minecraft.init.Blocks.air) return null;
        legacyconnector.v1_7_10.net.minecraft.util.AxisAlignedBB box = block.getCollisionBoundingBoxFromPool(world, x, y, z);
        if (box == null) return null;
        int[] dx = legacyconnector.v1_7_10.net.minecraft.util.Facing.offsetsXForSide;
        int[] dy = legacyconnector.v1_7_10.net.minecraft.util.Facing.offsetsYForSide;
        int[] dz = legacyconnector.v1_7_10.net.minecraft.util.Facing.offsetsZForSide;
        if (dx[direction] < 0) box.minX -= (double) ((float) dx[direction] * progress); else box.maxX -= (double) ((float) dx[direction] * progress);
        if (dy[direction] < 0) box.minY -= (double) ((float) dy[direction] * progress); else box.maxY -= (double) ((float) dy[direction] * progress);
        if (dz[direction] < 0) box.minZ -= (double) ((float) dz[direction] * progress); else box.maxZ -= (double) ((float) dz[direction] * progress);
        return box;
    }

    /** The moving-piston tile entity carrying {@code block} with {@code meta} towards {@code orientation}. */
    public static legacyconnector.v1_7_10.net.minecraft.tileentity.TileEntity getTileEntity(Block block, int meta, int orientation, boolean extending,
            boolean renderHead) {
        return new legacyconnector.v1_7_10.net.minecraft.tileentity.TileEntityPiston(block, meta, orientation, extending, renderHead);
    }
}
