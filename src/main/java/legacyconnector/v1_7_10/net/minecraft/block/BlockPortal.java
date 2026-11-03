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
