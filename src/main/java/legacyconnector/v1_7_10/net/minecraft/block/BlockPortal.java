package legacyconnector.v1_7_10.net.minecraft.block;

/**
 * Generated shell of the 1.7.10 class {@code net.minecraft.block.BlockPortal}: correct type hierarchy for {@code instanceof} checks and
 * subclassing, legacy constructors, inherited default behaviour. Vanilla-specific logic is not ported yet.
 * Regenerate with tools/gen_vanilla_shims.py; replace with a hand-written class to add behaviour.
 */
public class BlockPortal extends legacyconnector.v1_7_10.net.minecraft.block.BlockBreakable {
    public BlockPortal(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info) {
        super(info);
    }

    protected BlockPortal(legacyconnector.v1_7_10.net.minecraft.block.material.Material material) {
        super(material);
    }

    public BlockPortal() {
        this(legacyconnector.v1_7_10.net.minecraft.block.material.Material.portal);
    }

    /** Frame-size table by axis (1 = along x, 2 = along z): {width direction, other direction}; read by mods' portal-size helpers. */
    public static final int[][] field_150001_a = {new int[0], {3, 1}, {2, 0}};

    @Override
    public boolean isOpaqueCube() {
        return false;
    }

    @Override
    public boolean renderAsNormalBlock() {
        return false;
    }

    @Override
    public int quantityDropped(java.util.Random random) {
        return 0;
    }

    @Override
    public legacyconnector.v1_7_10.net.minecraft.util.AxisAlignedBB getCollisionBoundingBoxFromPool(
            legacyconnector.v1_7_10.net.minecraft.world.World world, int x, int y, int z) {
        return null;
    }

    /** The thin slab of 1.7.10's portal along its axis (metadata 1 = along x, 2 = along z, 0 = guess from neighbours). */
    @Override
    public void setBlockBoundsBasedOnState(legacyconnector.v1_7_10.net.minecraft.world.IBlockAccess world, int x, int y, int z) {
        int axis = func_149999_b(world.getBlockMetadata(x, y, z));
        if (axis == 0) axis = world.getBlock(x - 1, y, z) != this && world.getBlock(x + 1, y, z) != this ? 2 : 1;
        float hx = axis == 1 ? 0.5F : 0.125F;
        float hz = axis == 1 ? 0.125F : 0.5F;
        setBlockBounds(0.5F - hx, 0.0F, 0.5F - hz, 0.5F + hx, 1.0F, 0.5F + hz);
    }

    /** The orientation bits of a portal block's metadata (1 = along x, 2 = along z). */
    public static int func_149999_b(int meta) {
        return meta & 3;
    }

    /** Fills an obsidian frame around the position with portal blocks (either axis); false when there is no valid frame. */
    public boolean tryToCreatePortal(legacyconnector.v1_7_10.net.minecraft.world.World world, int x, int y, int z) {
        net.minecraft.world.level.Level level = world == null ? null : world.mcc$fullLevel();
        if (level == null) return false;
        net.minecraft.core.BlockPos pos = new net.minecraft.core.BlockPos(x, y, z);
        for (net.minecraft.core.Direction.Axis axis : new net.minecraft.core.Direction.Axis[] {
                net.minecraft.core.Direction.Axis.X, net.minecraft.core.Direction.Axis.Z}) {
            var shape = net.minecraft.world.level.portal.PortalShape.findEmptyPortalShape(level, pos, axis);
            if (shape.isPresent()) {
                shape.get().createPortalBlocks();
                return true;
            }
        }
        return false;
    }

    /** MCP name of {@link #tryToCreatePortal}. */
    public boolean func_150000_e(legacyconnector.v1_7_10.net.minecraft.world.World world, int x, int y, int z) {
        return tryToCreatePortal(world, x, y, z);
    }
}
