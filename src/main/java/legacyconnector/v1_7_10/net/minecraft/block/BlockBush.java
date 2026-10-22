package legacyconnector.v1_7_10.net.minecraft.block;

/**
 * Generated shell of the 1.7.10 class {@code net.minecraft.block.BlockBush}: correct type hierarchy for {@code instanceof} checks and
 * subclassing, legacy constructors, inherited default behaviour. Vanilla-specific logic is not ported yet.
 * Regenerate with tools/gen_vanilla_shims.py; replace with a hand-written class to add behaviour.
 */
public class BlockBush extends legacyconnector.v1_7_10.net.minecraft.block.Block implements legacyconnector.v1_7_10.net.minecraftforge.common.IPlantable {
    public BlockBush(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info) {
        super(info);
    }

    protected BlockBush(legacyconnector.v1_7_10.net.minecraft.block.material.Material material) {
        super(material);
    }

    protected BlockBush() {
        this(legacyconnector.v1_7_10.net.minecraft.block.material.Material.plants);
    }

    {
        // Vanilla's plant defaults (1.7.10 BlockBush constructor): ticks to check its support, a small cross-shaped box.
        setTickRandomly(true);
        float f = 0.2F;
        setBlockBounds(0.5F - f, 0.0F, 0.5F - f, 0.5F + f, f * 3.0F, 0.5F + f);
    }

    /** The blocks this plant may stand on. */
    protected boolean canPlaceBlockOn(legacyconnector.v1_7_10.net.minecraft.block.Block block) {
        return block == legacyconnector.v1_7_10.net.minecraft.init.Blocks.grass || block == legacyconnector.v1_7_10.net.minecraft.init.Blocks.dirt
                || block == legacyconnector.v1_7_10.net.minecraft.init.Blocks.farmland;
    }

    @Override
    public boolean canPlaceBlockAt(legacyconnector.v1_7_10.net.minecraft.world.World world, int x, int y, int z) {
        return super.canPlaceBlockAt(world, x, y, z) && canPlaceBlockOn(world.getBlock(x, y - 1, z));
    }

    @Override
    public boolean canBlockStay(legacyconnector.v1_7_10.net.minecraft.world.World world, int x, int y, int z) {
        return (world.getFullBlockLightValue(x, y, z) >= 8 || world.canBlockSeeTheSky(x, y, z)) && canPlaceBlockOn(world.getBlock(x, y - 1, z));
    }

    @Override
    public void onNeighborBlockChange(legacyconnector.v1_7_10.net.minecraft.world.World world, int x, int y, int z,
            legacyconnector.v1_7_10.net.minecraft.block.Block neighbor) {
        super.onNeighborBlockChange(world, x, y, z, neighbor);
        checkAndDropBlock(world, x, y, z);
    }

    @Override
    public void updateTick(legacyconnector.v1_7_10.net.minecraft.world.World world, int x, int y, int z, java.util.Random random) {
        checkAndDropBlock(world, x, y, z);
    }

    protected void checkAndDropBlock(legacyconnector.v1_7_10.net.minecraft.world.World world, int x, int y, int z) {
        if (!canBlockStay(world, x, y, z)) {
            dropBlockAsItem(world, x, y, z, world.getBlockMetadata(x, y, z), 0);
            world.setBlock(x, y, z, legacyconnector.v1_7_10.net.minecraft.init.Blocks.air, 0, 2);
        }
    }

    @Override
    public legacyconnector.v1_7_10.net.minecraft.util.AxisAlignedBB getCollisionBoundingBoxFromPool(legacyconnector.v1_7_10.net.minecraft.world.World world, int x,
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
    public legacyconnector.v1_7_10.net.minecraftforge.common.EnumPlantType getPlantType(legacyconnector.v1_7_10.net.minecraft.world.IBlockAccess world, int x, int y,
            int z) {
        return legacyconnector.v1_7_10.net.minecraftforge.common.EnumPlantType.Plains;
    }

    @Override
    public legacyconnector.v1_7_10.net.minecraft.block.Block getPlant(legacyconnector.v1_7_10.net.minecraft.world.IBlockAccess world, int x, int y, int z) {
        return this;
    }

    @Override
    public int getPlantMetadata(legacyconnector.v1_7_10.net.minecraft.world.IBlockAccess world, int x, int y, int z) {
        return world.getBlockMetadata(x, y, z);
    }
}
