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

    /** The strongest redstone signal arriving at the two sides of the diode (1.7.10's getPowerOnSides). */
    protected int func_149902_h(legacyconnector.v1_7_10.net.minecraft.world.IBlockAccess world, int x, int y, int z, int meta) {
        int dir = getDirection(meta);
        int dx = legacyconnector.v1_7_10.net.minecraft.util.Direction.offsetX[dir];
        int dz = legacyconnector.v1_7_10.net.minecraft.util.Direction.offsetZ[dir];
        int lx = x - dz, lz = z + dx, rx = x + dz, rz = z - dx;
        return Math.max(mcc$sideSignal(world, lx, y, lz, mcc$side(lx - x, lz - z)), mcc$sideSignal(world, rx, y, rz, mcc$side(rx - x, rz - z)));
    }

    private static int mcc$side(int ox, int oz) {
        return ox < 0 ? 4 : ox > 0 ? 5 : oz < 0 ? 2 : 3;
    }

    private static int mcc$sideSignal(legacyconnector.v1_7_10.net.minecraft.world.IBlockAccess world, int x, int y, int z, int side) {
        legacyconnector.v1_7_10.net.minecraft.block.Block b = world.getBlock(x, y, z);
        if (b == null) return 0;
        int p = b.isProvidingWeakPower(world, x, y, z, side);
        if (b == legacyconnector.v1_7_10.net.minecraft.init.Blocks.redstone_wire) p = Math.max(p, world.getBlockMetadata(x, y, z));
        return p;
    }

    /** Notifies the block the diode points at that its input changed (1.7.10's notifyNeighbors). */
    protected void func_149911_e(legacyconnector.v1_7_10.net.minecraft.world.World world, int x, int y, int z) {
        int dir = getDirection(world.getBlockMetadata(x, y, z));
        int nx = x - legacyconnector.v1_7_10.net.minecraft.util.Direction.offsetX[dir];
        int nz = z - legacyconnector.v1_7_10.net.minecraft.util.Direction.offsetZ[dir];
        world.notifyBlockOfNeighborChange(nx, y, nz, this);
        world.notifyBlocksOfNeighborChange(nx, y, nz, this, 4);
    }
}
