package legacyconnector.v1_7_10.net.minecraft.block;

import java.util.Random;
import legacyconnector.v1_7_10.net.minecraft.block.material.Material;
import legacyconnector.v1_7_10.net.minecraft.creativetab.CreativeTabs;
import legacyconnector.v1_7_10.net.minecraft.util.AxisAlignedBB;
import legacyconnector.v1_7_10.net.minecraft.world.IBlockAccess;
import legacyconnector.v1_7_10.net.minecraft.world.World;
import legacyconnector.v1_7_10.net.minecraftforge.common.util.ForgeDirection;

/** A ladder (1.7.10 {@code BlockLadder}): climbable, a thin board on the side of a block (metadata 2 north, 3 south, 4 west, 5 east wall), dropped when that block goes. */
public class BlockLadder extends Block {
    public BlockLadder(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info) {
        super(info);
    }

    public BlockLadder() {
        super(Material.circuits);
        setCreativeTab(CreativeTabs.tabDecorations);
    }

    @Override
    public boolean isLadder(IBlockAccess world, int x, int y, int z, legacyconnector.v1_7_10.net.minecraft.entity.EntityLivingBase entity) {
        return true;
    }

    @Override
    public AxisAlignedBB getCollisionBoundingBoxFromPool(World world, int x, int y, int z) {
        setBlockBoundsBasedOnState(world, x, y, z);
        return super.getCollisionBoundingBoxFromPool(world, x, y, z);
    }

    @Override
    public AxisAlignedBB getSelectedBoundingBoxFromPool(World world, int x, int y, int z) {
        setBlockBoundsBasedOnState(world, x, y, z);
        return super.getSelectedBoundingBoxFromPool(world, x, y, z);
    }

    @Override
    public void setBlockBoundsBasedOnState(IBlockAccess world, int x, int y, int z) {
        func_149797_b(world.getBlockMetadata(x, y, z));
    }

    /** The 1/8 thick board against the wall the metadata names. */
    public void func_149797_b(int meta) {
        float thickness = 0.125F;
        if (meta == 2) setBlockBounds(0.0F, 0.0F, 1.0F - thickness, 1.0F, 1.0F, 1.0F);
        if (meta == 3) setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, thickness);
        if (meta == 4) setBlockBounds(1.0F - thickness, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
        if (meta == 5) setBlockBounds(0.0F, 0.0F, 0.0F, thickness, 1.0F, 1.0F);
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
        return 8;
    }

    @Override
    public boolean canPlaceBlockAt(World world, int x, int y, int z) {
        if (mcc$vanilla != null) return super.canPlaceBlockAt(world, x, y, z);
        return world.isSideSolid(x - 1, y, z, ForgeDirection.EAST) || world.isSideSolid(x + 1, y, z, ForgeDirection.WEST)
                || world.isSideSolid(x, y, z - 1, ForgeDirection.SOUTH) || world.isSideSolid(x, y, z + 1, ForgeDirection.NORTH);
    }

    @Override
    public int onBlockPlaced(World world, int x, int y, int z, int side, float hitX, float hitY, float hitZ, int meta) {
        if (mcc$vanilla != null) return super.onBlockPlaced(world, x, y, z, side, hitX, hitY, hitZ, meta);
        int result = meta;
        if ((result == 0 || side == 2) && world.isSideSolid(x, y, z + 1, ForgeDirection.NORTH)) result = 2;
        if ((result == 0 || side == 3) && world.isSideSolid(x, y, z - 1, ForgeDirection.SOUTH)) result = 3;
        if ((result == 0 || side == 4) && world.isSideSolid(x + 1, y, z, ForgeDirection.WEST)) result = 4;
        if ((result == 0 || side == 5) && world.isSideSolid(x - 1, y, z, ForgeDirection.EAST)) result = 5;
        return result;
    }

    @Override
    public void onNeighborBlockChange(World world, int x, int y, int z, Block neighbor) {
        if (mcc$vanilla == null) {
            int meta = world.getBlockMetadata(x, y, z);
            boolean supported = false;
            if (meta == 2 && world.isSideSolid(x, y, z + 1, ForgeDirection.NORTH)) supported = true;
            if (meta == 3 && world.isSideSolid(x, y, z - 1, ForgeDirection.SOUTH)) supported = true;
            if (meta == 4 && world.isSideSolid(x + 1, y, z, ForgeDirection.WEST)) supported = true;
            if (meta == 5 && world.isSideSolid(x - 1, y, z, ForgeDirection.EAST)) supported = true;
            if (!supported) {
                dropBlockAsItem(world, x, y, z, meta, 0);
                world.setBlockToAir(x, y, z);
            }
        }
        super.onNeighborBlockChange(world, x, y, z, neighbor);
    }

    @Override
    public int quantityDropped(Random random) {
        return 1;
    }
}
