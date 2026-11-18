package legacyconnector.v1_7_10.net.minecraft.block;

import java.util.Random;
import legacyconnector.v1_7_10.net.minecraft.block.material.Material;
import legacyconnector.v1_7_10.net.minecraft.creativetab.CreativeTabs;
import legacyconnector.v1_7_10.net.minecraft.util.AxisAlignedBB;
import legacyconnector.v1_7_10.net.minecraft.util.MovingObjectPosition;
import legacyconnector.v1_7_10.net.minecraft.util.Vec3;
import legacyconnector.v1_7_10.net.minecraft.world.IBlockAccess;
import legacyconnector.v1_7_10.net.minecraft.world.World;
import legacyconnector.v1_7_10.net.minecraftforge.common.util.ForgeDirection;

/**
 * A wall or floor torch (and the base of mods' torches), as in 1.7.10: metadata 1 to 4 is the wall it is stuck on (west, east, north, south neighbour),
 * 5 the floor; it needs a solid face to stand on and drops when that goes. The thin box depends on the metadata.
 */
public class BlockTorch extends Block {
    public BlockTorch(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info) {
        super(info);
    }

    protected BlockTorch() {
        this(Material.circuits);
    }

    protected BlockTorch(Material material) {
        super(material);
        setTickRandomly(true);
        setCreativeTab(CreativeTabs.tabDecorations);
    }

    /** Whether a torch can stand on the block at the position: it has a solid top surface (or the block says torches may sit on it). */
    public boolean canPlaceTorchOn(legacyconnector.v1_7_10.net.minecraft.world.World world, int x, int y, int z) {
        if (legacyconnector.v1_7_10.net.minecraft.world.World.doesBlockHaveSolidTopSurface(world, x, y, z)) return true;
        Block block = world.getBlock(x, y, z);
        return block != null && block.canPlaceTorchOnTop(world, x, y, z);
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
        return 2;
    }

    @Override
    public boolean canPlaceBlockAt(World world, int x, int y, int z) {
        if (mcc$vanilla != null) return super.canPlaceBlockAt(world, x, y, z);
        return world.isSideSolid(x - 1, y, z, ForgeDirection.EAST, true) || world.isSideSolid(x + 1, y, z, ForgeDirection.WEST, true)
                || world.isSideSolid(x, y, z - 1, ForgeDirection.SOUTH, true) || world.isSideSolid(x, y, z + 1, ForgeDirection.NORTH, true)
                || canPlaceTorchOn(world, x, y - 1, z);
    }

    @Override
    public int onBlockPlaced(World world, int x, int y, int z, int side, float hitX, float hitY, float hitZ, int meta) {
        if (mcc$vanilla != null) return super.onBlockPlaced(world, x, y, z, side, hitX, hitY, hitZ, meta);
        int result = meta;
        if (side == 1 && canPlaceTorchOn(world, x, y - 1, z)) result = 5;
        if (side == 2 && world.isSideSolid(x, y, z + 1, ForgeDirection.NORTH, true)) result = 4;
        if (side == 3 && world.isSideSolid(x, y, z - 1, ForgeDirection.SOUTH, true)) result = 3;
        if (side == 4 && world.isSideSolid(x + 1, y, z, ForgeDirection.WEST, true)) result = 2;
        if (side == 5 && world.isSideSolid(x - 1, y, z, ForgeDirection.EAST, true)) result = 1;
        return result;
    }

    @Override
    public void updateTick(World world, int x, int y, int z, Random random) {
        super.updateTick(world, x, y, z, random);
        if (mcc$vanilla == null && world.getBlockMetadata(x, y, z) == 0) onBlockAdded(world, x, y, z);
    }

    @Override
    public void onBlockAdded(World world, int x, int y, int z) {
        if (mcc$vanilla != null) {
            super.onBlockAdded(world, x, y, z);
            return;
        }
        if (world.getBlockMetadata(x, y, z) == 0) {
            if (world.isSideSolid(x - 1, y, z, ForgeDirection.EAST, true)) world.setBlockMetadataWithNotify(x, y, z, 1, 2);
            else if (world.isSideSolid(x + 1, y, z, ForgeDirection.WEST, true)) world.setBlockMetadataWithNotify(x, y, z, 2, 2);
            else if (world.isSideSolid(x, y, z - 1, ForgeDirection.SOUTH, true)) world.setBlockMetadataWithNotify(x, y, z, 3, 2);
            else if (world.isSideSolid(x, y, z + 1, ForgeDirection.NORTH, true)) world.setBlockMetadataWithNotify(x, y, z, 4, 2);
            else if (canPlaceTorchOn(world, x, y - 1, z)) world.setBlockMetadataWithNotify(x, y, z, 5, 2);
        }
        dropTorchIfCantStay(world, x, y, z);
    }

    @Override
    public void onNeighborBlockChange(World world, int x, int y, int z, Block neighbor) {
        if (mcc$vanilla != null) {
            super.onNeighborBlockChange(world, x, y, z, neighbor);
            return;
        }
        func_150108_b(world, x, y, z, neighbor);
    }

    /** Drops the torch when the face it hangs on is gone; true while it stays. */
    protected boolean func_150108_b(World world, int x, int y, int z, Block neighbor) {
        if (!dropTorchIfCantStay(world, x, y, z)) return true;
        int meta = world.getBlockMetadata(x, y, z);
        boolean gone = false;
        if (!world.isSideSolid(x - 1, y, z, ForgeDirection.EAST, true) && meta == 1) gone = true;
        if (!world.isSideSolid(x + 1, y, z, ForgeDirection.WEST, true) && meta == 2) gone = true;
        if (!world.isSideSolid(x, y, z - 1, ForgeDirection.SOUTH, true) && meta == 3) gone = true;
        if (!world.isSideSolid(x, y, z + 1, ForgeDirection.NORTH, true) && meta == 4) gone = true;
        if (!canPlaceTorchOn(world, x, y - 1, z) && meta == 5) gone = true;
        if (!gone) return false;
        dropBlockAsItem(world, x, y, z, meta, 0);
        world.setBlockToAir(x, y, z);
        return true;
    }

    /** Drops the torch when it can no longer be placed where it is; true while it can. */
    protected boolean dropTorchIfCantStay(World world, int x, int y, int z) {
        if (canPlaceBlockAt(world, x, y, z)) return true;
        if (world.getBlock(x, y, z) == this) {
            dropBlockAsItem(world, x, y, z, world.getBlockMetadata(x, y, z), 0);
            world.setBlockToAir(x, y, z);
        }
        return false;
    }

    /** The thin box of a torch by metadata: leaning off its wall, or upright on the floor. */
    protected void mcc$setTorchBounds(int meta) {
        float width = 0.15F;
        switch (meta & 7) {
            case 1 -> setBlockBounds(0.0F, 0.2F, 0.5F - width, width * 2.0F, 0.8F, 0.5F + width);
            case 2 -> setBlockBounds(1.0F - width * 2.0F, 0.2F, 0.5F - width, 1.0F, 0.8F, 0.5F + width);
            case 3 -> setBlockBounds(0.5F - width, 0.2F, 0.0F, 0.5F + width, 0.8F, width * 2.0F);
            case 4 -> setBlockBounds(0.5F - width, 0.2F, 1.0F - width * 2.0F, 0.5F + width, 0.8F, 1.0F);
            default -> {
                width = 0.1F;
                setBlockBounds(0.5F - width, 0.0F, 0.5F - width, 0.5F + width, 0.6F, 0.5F + width);
            }
        }
    }

    @Override
    public void setBlockBoundsBasedOnState(IBlockAccess world, int x, int y, int z) {
        if (mcc$vanilla != null) {
            super.setBlockBoundsBasedOnState(world, x, y, z);
            return;
        }
        mcc$setTorchBounds(world.getBlockMetadata(x, y, z));
    }

    @Override
    public MovingObjectPosition collisionRayTrace(World world, int x, int y, int z, Vec3 start, Vec3 end) {
        if (mcc$vanilla == null) mcc$setTorchBounds(world.getBlockMetadata(x, y, z));
        return super.collisionRayTrace(world, x, y, z, start, end);
    }
}
