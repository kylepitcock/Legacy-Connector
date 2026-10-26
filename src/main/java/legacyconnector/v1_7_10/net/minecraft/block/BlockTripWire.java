package legacyconnector.v1_7_10.net.minecraft.block;

/** The tripwire string between two hooks (metadata: 1 powered, 2 suspended, 4 attached); RenderBlocks draws it (render type 30). */
public class BlockTripWire extends Block {
    public BlockTripWire(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info) {
        super(info);
    }

    public BlockTripWire() {
        super(legacyconnector.v1_7_10.net.minecraft.block.material.Material.circuits);
        setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 0.15625F, 1.0F);
        setTickRandomly(true);
    }

    @Override
    public boolean isOpaqueCube() {
        return false;
    }

    @Override
    public boolean renderAsNormalBlock() {
        return false;
    }

    @Override
    public int getRenderBlockPass() {
        return 1;
    }

    @Override
    public int getRenderType() {
        return 30;
    }

    @Override
    public void setBlockBoundsBasedOnState(legacyconnector.v1_7_10.net.minecraft.world.IBlockAccess world, int x, int y, int z) {
        int meta = world.getBlockMetadata(x, y, z);
        boolean attached = (meta & 4) == 4;
        boolean suspended = (meta & 2) == 2;
        if (!suspended) setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 0.09375F, 1.0F);
        else if (!attached) setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 0.5F, 1.0F);
        else setBlockBounds(0.0F, 0.0625F, 0.0F, 1.0F, 0.15625F, 1.0F);
    }

    /** Whether the wire at a position connects towards {@code direction} (0-3): to a hook facing it or to wire in the same state. */
    public static boolean func_150139_a(legacyconnector.v1_7_10.net.minecraft.world.IBlockAccess world, int x, int y, int z, int meta, int direction) {
        int nx = x + legacyconnector.v1_7_10.net.minecraft.util.Direction.offsetX[direction];
        int nz = z + legacyconnector.v1_7_10.net.minecraft.util.Direction.offsetZ[direction];
        Block block = world.getBlock(nx, y, nz);
        boolean suspended = (meta & 2) == 2;
        if (block == legacyconnector.v1_7_10.net.minecraft.init.Blocks.tripwire_hook) {
            return (world.getBlockMetadata(nx, y, nz) & 3) == legacyconnector.v1_7_10.net.minecraft.util.Direction.rotateOpposite[direction];
        }
        if (block == legacyconnector.v1_7_10.net.minecraft.init.Blocks.tripwire || block instanceof BlockTripWire) {
            return suspended == ((world.getBlockMetadata(nx, y, nz) & 2) == 2);
        }
        return false;
    }
}
