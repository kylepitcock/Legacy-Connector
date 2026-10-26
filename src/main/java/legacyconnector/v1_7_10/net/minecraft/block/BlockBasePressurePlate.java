package legacyconnector.v1_7_10.net.minecraft.block;

import java.util.Random;
import legacyconnector.v1_7_10.net.minecraft.block.material.Material;
import legacyconnector.v1_7_10.net.minecraft.util.AxisAlignedBB;
import legacyconnector.v1_7_10.net.minecraft.world.IBlockAccess;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/**
 * 1.7.10's pressure plate logic: entities on the plate ({@link #getPlateState}) power it, the power is kept in the metadata
 * ({@link #getMetaFromPower}/{@link #getPowerFromMeta}) and rechecked every {@link #tickRate} ticks while pressed. Vanilla plates
 * work through the modern blocks; mods' plates run this logic.
 */
public class BlockBasePressurePlate extends Block {
    private String field_150067_a;

    public BlockBasePressurePlate(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info) {
        super(info);
    }

    protected BlockBasePressurePlate(String texture, Material material) {
        super(material);
        this.field_150067_a = texture;
        setCreativeTab(legacyconnector.v1_7_10.net.minecraft.creativetab.CreativeTabs.tabRedstone);
        setTickRandomly(true);
        setBlockBoundsFromMeta(getMetaFromPower(15));
    }

    @Override
    public void setBlockBoundsBasedOnState(IBlockAccess world, int x, int y, int z) {
        setBlockBoundsFromMeta(world.getBlockMetadata(x, y, z));
    }

    /** {@code func_150063_b}: a pressed plate is half as high. */
    protected void setBlockBoundsFromMeta(int meta) {
        float f = 0.0625F;
        if (getPowerFromMeta(meta) > 0) setBlockBounds(f, 0.0F, f, 1.0F - f, 0.03125F, 1.0F - f);
        else setBlockBounds(f, 0.0F, f, 1.0F - f, 0.0625F, 1.0F - f);
    }

    @Override
    public int tickRate(World world) {
        return 20;
    }

    @Override
    public AxisAlignedBB getCollisionBoundingBoxFromPool(World world, int x, int y, int z) {
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
    public boolean getBlocksMovement(IBlockAccess world, int x, int y, int z) {
        return true;
    }

    @Override
    public boolean canPlaceBlockAt(World world, int x, int y, int z) {
        return World.doesBlockHaveSolidTopSurface(world, x, y - 1, z) || BlockFence.func_149825_a(world.getBlock(x, y - 1, z));
    }

    @Override
    public void onNeighborBlockChange(World world, int x, int y, int z, Block neighbor) {
        if (mcc$vanilla != null) return;
        if (!World.doesBlockHaveSolidTopSurface(world, x, y - 1, z) && !BlockFence.func_149825_a(world.getBlock(x, y - 1, z))) {
            dropBlockAsItem(world, x, y, z, world.getBlockMetadata(x, y, z), 0);
            world.setBlockToAir(x, y, z);
        }
    }

    @Override
    public void updateTick(World world, int x, int y, int z, Random random) {
        if (mcc$vanilla != null) {
            super.updateTick(world, x, y, z, random);
            return;
        }
        if (world.isRemote) return;
        int power = getPowerFromMeta(world.getBlockMetadata(x, y, z));
        if (power > 0) setStateIfMobInteractsWithPlate(world, x, y, z, power);
    }

    @Override
    public void onEntityCollidedWithBlock(World world, int x, int y, int z, legacyconnector.v1_7_10.net.minecraft.entity.Entity entity) {
        if (mcc$vanilla != null || world.isRemote) return;
        int power = getPowerFromMeta(world.getBlockMetadata(x, y, z));
        if (power == 0) setStateIfMobInteractsWithPlate(world, x, y, z, power);
    }

    /** {@code func_150062_a}: recomputes the plate's power, clicks when it changes and keeps checking while pressed. */
    protected void setStateIfMobInteractsWithPlate(World world, int x, int y, int z, int oldPower) {
        int power = getPlateState(world, x, y, z);
        boolean was = oldPower > 0;
        boolean now = power > 0;
        if (oldPower != power) {
            world.setBlockMetadataWithNotify(x, y, z, getMetaFromPower(power), 2);
            updateNeighbors(world, x, y, z);
            world.markBlockRangeForRenderUpdate(x, y, z, x, y, z);
        }
        if (!now && was) world.playSoundEffect(x + 0.5D, y + 0.1D, z + 0.5D, "random.click", 0.3F, 0.5F);
        else if (now && !was) world.playSoundEffect(x + 0.5D, y + 0.1D, z + 0.5D, "random.click", 0.3F, 0.6F);
        if (now) world.scheduleBlockUpdate(x, y, z, this, tickRate(world));
    }

    /** {@code func_150061_a}: the box entities must touch to press the plate. */
    protected AxisAlignedBB getSensitiveAABB(int x, int y, int z) {
        float f = 0.125F;
        return AxisAlignedBB.getBoundingBox(x + f, y, z + f, x + 1 - f, y + 0.25D, z + 1 - f);
    }

    @Override
    public void breakBlock(World world, int x, int y, int z, Block block, int meta) {
        if (mcc$vanilla == null && getPowerFromMeta(meta) > 0) updateNeighbors(world, x, y, z);
        super.breakBlock(world, x, y, z, block, meta);
    }

    /** {@code func_150064_a_}: tells the neighbours (and the block below) that the plate's power changed. */
    protected void updateNeighbors(World world, int x, int y, int z) {
        world.notifyBlocksOfNeighborChange(x, y, z, this);
        world.notifyBlocksOfNeighborChange(x, y - 1, z, this);
    }

    @Override
    public int isProvidingWeakPower(IBlockAccess world, int x, int y, int z, int side) {
        return getPowerFromMeta(world.getBlockMetadata(x, y, z));
    }

    @Override
    public int isProvidingStrongPower(IBlockAccess world, int x, int y, int z, int side) {
        return side == 1 ? getPowerFromMeta(world.getBlockMetadata(x, y, z)) : 0;
    }

    @Override
    public boolean canProvidePower() {
        return true;
    }

    @Override
    public void setBlockBoundsForItemRender() {
        setBlockBounds(0.0F, 0.375F, 0.0F, 1.0F, 0.625F, 1.0F);
    }

    @Override
    public int getMobilityFlag() {
        return 1;
    }

    @Override
    public void registerBlockIcons(legacyconnector.v1_7_10.net.minecraft.client.renderer.texture.IIconRegister register) {
        blockIcon = register.registerIcon(field_150067_a != null ? field_150067_a : getTextureName());
    }

    /** {@code func_150065_e}: the power (0-15) the entities on the plate give it. */
    protected int getPlateState(World world, int x, int y, int z) {
        return 0;
    }

    /** {@code func_150060_c}. */
    protected int getPowerFromMeta(int meta) {
        return meta == 1 ? 15 : 0;
    }

    /** {@code func_150066_d}. */
    protected int getMetaFromPower(int power) {
        return power > 0 ? 1 : 0;
    }
}
