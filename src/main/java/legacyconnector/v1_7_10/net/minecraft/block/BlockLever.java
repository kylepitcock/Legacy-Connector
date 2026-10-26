package legacyconnector.v1_7_10.net.minecraft.block;

import legacyconnector.v1_7_10.net.minecraft.block.material.Material;
import legacyconnector.v1_7_10.net.minecraft.entity.Entity;
import legacyconnector.v1_7_10.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;
import legacyconnector.v1_7_10.net.minecraft.util.AxisAlignedBB;
import legacyconnector.v1_7_10.net.minecraft.util.MathHelper;
import legacyconnector.v1_7_10.net.minecraft.world.IBlockAccess;
import legacyconnector.v1_7_10.net.minecraft.world.World;
import legacyconnector.v1_7_10.net.minecraftforge.common.util.ForgeDirection;

/**
 * A lever, ported from 1.7.10: metadata 1-4 is the wall it is on (east, west, south, north face of the neighbour), 5/6 the
 * floor and 0/7 the ceiling (along z or x), bit 8 on. When on it strongly powers the block it is attached to.
 */
public class BlockLever extends Block {
    public BlockLever(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info) {
        super(info);
    }

    protected BlockLever() {
        super(Material.circuits);
        setCreativeTab(legacyconnector.v1_7_10.net.minecraft.creativetab.CreativeTabs.tabRedstone);
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
    public int getRenderType() {
        return 12;
    }

    @Override
    public boolean canPlaceBlockOnSide(World world, int x, int y, int z, int side) {
        ForgeDirection dir = ForgeDirection.getOrientation(side);
        return dir == ForgeDirection.DOWN && world.isSideSolid(x, y + 1, z, ForgeDirection.DOWN)
                || dir == ForgeDirection.UP && world.isSideSolid(x, y - 1, z, ForgeDirection.UP)
                || dir == ForgeDirection.NORTH && world.isSideSolid(x, y, z + 1, ForgeDirection.NORTH)
                || dir == ForgeDirection.SOUTH && world.isSideSolid(x, y, z - 1, ForgeDirection.SOUTH)
                || dir == ForgeDirection.WEST && world.isSideSolid(x + 1, y, z, ForgeDirection.WEST)
                || dir == ForgeDirection.EAST && world.isSideSolid(x - 1, y, z, ForgeDirection.EAST);
    }

    @Override
    public boolean canPlaceBlockAt(World world, int x, int y, int z) {
        return world.isSideSolid(x - 1, y, z, ForgeDirection.EAST) || world.isSideSolid(x + 1, y, z, ForgeDirection.WEST)
                || world.isSideSolid(x, y, z - 1, ForgeDirection.SOUTH) || world.isSideSolid(x, y, z + 1, ForgeDirection.NORTH)
                || world.isSideSolid(x, y - 1, z, ForgeDirection.UP) || world.isSideSolid(x, y + 1, z, ForgeDirection.DOWN);
    }

    @Override
    public int onBlockPlaced(World world, int x, int y, int z, int side, float hitX, float hitY, float hitZ, int meta) {
        int on = meta & 8;
        int facing = -1;
        if (side == 0 && world.isSideSolid(x, y + 1, z, ForgeDirection.DOWN)) facing = 0;
        if (side == 1 && world.isSideSolid(x, y - 1, z, ForgeDirection.UP)) facing = 5;
        if (side == 2 && world.isSideSolid(x, y, z + 1, ForgeDirection.NORTH)) facing = 4;
        if (side == 3 && world.isSideSolid(x, y, z - 1, ForgeDirection.SOUTH)) facing = 3;
        if (side == 4 && world.isSideSolid(x + 1, y, z, ForgeDirection.WEST)) facing = 2;
        if (side == 5 && world.isSideSolid(x - 1, y, z, ForgeDirection.EAST)) facing = 1;
        return facing + on;
    }

    @Override
    public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase placer, ItemStack stack) {
        int meta = world.getBlockMetadata(x, y, z);
        int facing = meta & 7;
        int on = meta & 8;
        boolean alongZ = (MathHelper.floor_double(Entity.mcc$get$rotationYaw(placer) * 4.0F / 360.0F + 0.5D) & 1) == 0;
        if (facing == invertMetadata(1)) world.setBlockMetadataWithNotify(x, y, z, (alongZ ? 5 : 6) | on, 2);
        else if (facing == invertMetadata(0)) world.setBlockMetadataWithNotify(x, y, z, (alongZ ? 7 : 0) | on, 2);
    }

    /** The lever metadata for a side it was placed against. */
    public static int invertMetadata(int side) {
        return switch (side) {
            case 0 -> 0;
            case 1 -> 5;
            case 2 -> 4;
            case 3 -> 3;
            case 4 -> 2;
            case 5 -> 1;
            default -> -1;
        };
    }

    @Override
    public void onNeighborBlockChange(World world, int x, int y, int z, Block neighbor) {
        if (!func_149820_e(world, x, y, z)) return;
        int facing = world.getBlockMetadata(x, y, z) & 7;
        boolean lost = !world.isSideSolid(x - 1, y, z, ForgeDirection.EAST) && facing == 1
                || !world.isSideSolid(x + 1, y, z, ForgeDirection.WEST) && facing == 2
                || !world.isSideSolid(x, y, z - 1, ForgeDirection.SOUTH) && facing == 3
                || !world.isSideSolid(x, y, z + 1, ForgeDirection.NORTH) && facing == 4
                || !world.isSideSolid(x, y - 1, z, ForgeDirection.UP) && (facing == 5 || facing == 6)
                || !world.isSideSolid(x, y + 1, z, ForgeDirection.DOWN) && (facing == 0 || facing == 7);
        if (lost) {
            dropBlockAsItem(world, x, y, z, world.getBlockMetadata(x, y, z), 0);
            world.setBlockToAir(x, y, z);
        }
    }

    /** Drops the lever when nothing is left to attach to; false then. */
    private boolean func_149820_e(World world, int x, int y, int z) {
        if (canPlaceBlockAt(world, x, y, z)) return true;
        dropBlockAsItem(world, x, y, z, world.getBlockMetadata(x, y, z), 0);
        world.setBlockToAir(x, y, z);
        return false;
    }

    @Override
    public void setBlockBoundsBasedOnState(IBlockAccess world, int x, int y, int z) {
        int facing = world.getBlockMetadata(x, y, z) & 7;
        float f = 0.1875F;
        if (facing == 1) setBlockBounds(0.0F, 0.2F, 0.5F - f, f * 2.0F, 0.8F, 0.5F + f);
        else if (facing == 2) setBlockBounds(1.0F - f * 2.0F, 0.2F, 0.5F - f, 1.0F, 0.8F, 0.5F + f);
        else if (facing == 3) setBlockBounds(0.5F - f, 0.2F, 0.0F, 0.5F + f, 0.8F, f * 2.0F);
        else if (facing == 4) setBlockBounds(0.5F - f, 0.2F, 1.0F - f * 2.0F, 0.5F + f, 0.8F, 1.0F);
        else if (facing == 5 || facing == 6) setBlockBounds(0.25F, 0.0F, 0.25F, 0.75F, 0.6F, 0.75F);
        else setBlockBounds(0.25F, 0.4F, 0.25F, 0.75F, 1.0F, 0.75F);
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX, float hitY, float hitZ) {
        if (world.isRemote) return true;
        int meta = world.getBlockMetadata(x, y, z);
        int facing = meta & 7;
        int toggled = 8 - (meta & 8);
        world.setBlockMetadataWithNotify(x, y, z, facing + toggled, 3);
        world.playSoundEffect(x + 0.5D, y + 0.5D, z + 0.5D, "random.click", 0.3F, toggled > 0 ? 0.6F : 0.5F);
        mcc$notifyAttached(world, x, y, z, facing);
        return true;
    }

    @Override
    public void breakBlock(World world, int x, int y, int z, Block block, int meta) {
        if ((meta & 8) > 0) mcc$notifyAttached(world, x, y, z, meta & 7);
        super.breakBlock(world, x, y, z, block, meta);
    }

    /** Updates the neighbours of the lever and of the block it is attached to. */
    private void mcc$notifyAttached(World world, int x, int y, int z, int facing) {
        world.notifyBlocksOfNeighborChange(x, y, z, this);
        switch (facing) {
            case 1 -> world.notifyBlocksOfNeighborChange(x - 1, y, z, this);
            case 2 -> world.notifyBlocksOfNeighborChange(x + 1, y, z, this);
            case 3 -> world.notifyBlocksOfNeighborChange(x, y, z - 1, this);
            case 4 -> world.notifyBlocksOfNeighborChange(x, y, z + 1, this);
            case 5, 6 -> world.notifyBlocksOfNeighborChange(x, y - 1, z, this);
            default -> world.notifyBlocksOfNeighborChange(x, y + 1, z, this);
        }
    }

    @Override
    public int isProvidingWeakPower(IBlockAccess world, int x, int y, int z, int side) {
        return (world.getBlockMetadata(x, y, z) & 8) > 0 ? 15 : 0;
    }

    @Override
    public int isProvidingStrongPower(IBlockAccess world, int x, int y, int z, int side) {
        int meta = world.getBlockMetadata(x, y, z);
        if ((meta & 8) == 0) return 0;
        int facing = meta & 7;
        boolean strong = (facing == 0 || facing == 7) && side == 0 || (facing == 5 || facing == 6) && side == 1 || facing == 4 && side == 2
                || facing == 3 && side == 3 || facing == 2 && side == 4 || facing == 1 && side == 5;
        return strong ? 15 : 0;
    }

    @Override
    public boolean canProvidePower() {
        return true;
    }
}
