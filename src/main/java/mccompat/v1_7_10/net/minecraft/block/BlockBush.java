package mccompat.v1_7_10.net.minecraft.block;

/**
 * Generated shell of the 1.7.10 class {@code net.minecraft.block.BlockBush}: correct type hierarchy for {@code instanceof} checks and
 * subclassing, legacy constructors, inherited default behaviour. Vanilla-specific logic is not ported yet.
 * Regenerate with tools/gen_vanilla_shims.py; replace with a hand-written class to add behaviour.
 */
public class BlockBush extends mccompat.v1_7_10.net.minecraft.block.Block implements mccompat.v1_7_10.net.minecraftforge.common.IPlantable {
    public BlockBush(org.kylepitcock.mccompat.v1_7_10.bridge.VanillaInfo info) {
        super(info);
    }

    protected BlockBush(mccompat.v1_7_10.net.minecraft.block.material.Material material) {
        super(material);
    }

    protected BlockBush() {
        this(mccompat.v1_7_10.net.minecraft.block.material.Material.plants);
    }

    {
        // Vanilla's plant defaults (1.7.10 BlockBush constructor): ticks to check its support, a small cross-shaped box.
        setTickRandomly(true);
        float f = 0.2F;
        setBlockBounds(0.5F - f, 0.0F, 0.5F - f, 0.5F + f, f * 3.0F, 0.5F + f);
    }

    /** The blocks this plant may stand on. */
    protected boolean canPlaceBlockOn(mccompat.v1_7_10.net.minecraft.block.Block block) {
        return block == mccompat.v1_7_10.net.minecraft.init.Blocks.grass || block == mccompat.v1_7_10.net.minecraft.init.Blocks.dirt
                || block == mccompat.v1_7_10.net.minecraft.init.Blocks.farmland;
    }

    @Override
    public boolean canPlaceBlockAt(mccompat.v1_7_10.net.minecraft.world.World world, int x, int y, int z) {
        return super.canPlaceBlockAt(world, x, y, z) && canPlaceBlockOn(world.getBlock(x, y - 1, z));
    }

    @Override
    public boolean canBlockStay(mccompat.v1_7_10.net.minecraft.world.World world, int x, int y, int z) {
        return (world.getFullBlockLightValue(x, y, z) >= 8 || world.canBlockSeeTheSky(x, y, z)) && canPlaceBlockOn(world.getBlock(x, y - 1, z));
    }

    @Override
    public void onNeighborBlockChange(mccompat.v1_7_10.net.minecraft.world.World world, int x, int y, int z,
            mccompat.v1_7_10.net.minecraft.block.Block neighbor) {
        super.onNeighborBlockChange(world, x, y, z, neighbor);
        checkAndDropBlock(world, x, y, z);
    }

    @Override
    public void updateTick(mccompat.v1_7_10.net.minecraft.world.World world, int x, int y, int z, java.util.Random random) {
        checkAndDropBlock(world, x, y, z);
    }

    protected void checkAndDropBlock(mccompat.v1_7_10.net.minecraft.world.World world, int x, int y, int z) {
        if (!canBlockStay(world, x, y, z)) {
            dropBlockAsItem(world, x, y, z, world.getBlockMetadata(x, y, z), 0);
            world.setBlock(x, y, z, mccompat.v1_7_10.net.minecraft.init.Blocks.air, 0, 2);
        }
    }

    @Override
    public mccompat.v1_7_10.net.minecraft.util.AxisAlignedBB getCollisionBoundingBoxFromPool(mccompat.v1_7_10.net.minecraft.world.World world, int x,
            int y, int z) {
        return null;
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
    public mccompat.v1_7_10.net.minecraftforge.common.EnumPlantType getPlantType(mccompat.v1_7_10.net.minecraft.world.IBlockAccess world, int x, int y,
            int z) {
        return mccompat.v1_7_10.net.minecraftforge.common.EnumPlantType.Plains;
    }

    @Override
    public mccompat.v1_7_10.net.minecraft.block.Block getPlant(mccompat.v1_7_10.net.minecraft.world.IBlockAccess world, int x, int y, int z) {
        return this;
    }

    @Override
    public int getPlantMetadata(mccompat.v1_7_10.net.minecraft.world.IBlockAccess world, int x, int y, int z) {
        return world.getBlockMetadata(x, y, z);
    }
}
