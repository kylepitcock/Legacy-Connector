package legacyconnector.v1_7_10.net.minecraft.block;

import legacyconnector.v1_7_10.net.minecraft.block.material.Material;
import legacyconnector.v1_7_10.net.minecraft.creativetab.CreativeTabs;
import legacyconnector.v1_7_10.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;
import legacyconnector.v1_7_10.net.minecraft.util.AxisAlignedBB;
import legacyconnector.v1_7_10.net.minecraft.util.IIcon;
import legacyconnector.v1_7_10.net.minecraft.util.MathHelper;
import legacyconnector.v1_7_10.net.minecraft.world.IBlockAccess;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/**
 * A gate between fences (1.7.10 {@code BlockFenceGate}): metadata bits 0-1 the facing, bit 4 open. It opens by right-click or power, blocks movement while
 * shut (a 1.5 block high collision box) and needs a solid block below.
 */
public class BlockFenceGate extends BlockDirectional {
    public BlockFenceGate(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info) {
        super(info);
    }

    public BlockFenceGate() {
        super(Material.wood);
        setCreativeTab(CreativeTabs.tabRedstone);
    }

    public static boolean isFenceGateOpen(int meta) {
        return (meta & 4) != 0;
    }

    @Override
    public IIcon getIcon(int side, int meta) {
        return legacyconnector.v1_7_10.net.minecraft.init.Blocks.planks.getIcon(side, 0);
    }

    @Override
    public boolean canPlaceBlockAt(World world, int x, int y, int z) {
        if (mcc$vanilla != null) return super.canPlaceBlockAt(world, x, y, z);
        return world.getBlock(x, y - 1, z).getMaterial().isSolid() && super.canPlaceBlockAt(world, x, y, z);
    }

    @Override
    public AxisAlignedBB getCollisionBoundingBoxFromPool(World world, int x, int y, int z) {
        int meta = world.getBlockMetadata(x, y, z);
        if (isFenceGateOpen(meta)) return null;
        return meta != 2 && meta != 0 ? AxisAlignedBB.getBoundingBox(x + 0.375F, y, z, x + 0.625F, y + 1.5F, z + 1)
                : AxisAlignedBB.getBoundingBox(x, y, z + 0.375F, x + 1, y + 1.5F, z + 0.625F);
    }

    @Override
    public void setBlockBoundsBasedOnState(IBlockAccess world, int x, int y, int z) {
        int direction = getDirection(world.getBlockMetadata(x, y, z));
        if (direction != 2 && direction != 0) setBlockBounds(0.375F, 0.0F, 0.0F, 0.625F, 1.0F, 1.0F);
        else setBlockBounds(0.0F, 0.0F, 0.375F, 1.0F, 1.0F, 0.625F);
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
        return isFenceGateOpen(world.getBlockMetadata(x, y, z));
    }

    @Override
    public int getRenderType() {
        return 21;
    }

    @Override
    public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase placer, ItemStack stack) {
        int facing = (MathHelper.floor_double(legacyconnector.v1_7_10.net.minecraft.entity.Entity.mcc$get$rotationYaw(placer) * 4.0F / 360.0F + 0.5D) & 3) % 4;
        world.setBlockMetadataWithNotify(x, y, z, facing, 2);
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX, float hitY, float hitZ) {
        int meta = world.getBlockMetadata(x, y, z);
        if (isFenceGateOpen(meta)) {
            world.setBlockMetadataWithNotify(x, y, z, meta & -5, 2);
        } else {
            int playerFacing = (MathHelper.floor_double(legacyconnector.v1_7_10.net.minecraft.entity.Entity.mcc$get$rotationYaw(player) * 4.0F / 360.0F + 0.5D) & 3) % 4;
            if (getDirection(meta) == (playerFacing + 2) % 4) meta = playerFacing;
            world.setBlockMetadataWithNotify(x, y, z, meta | 4, 2);
        }
        world.playAuxSFXAtEntity(player, 1003, x, y, z, 0);
        return true;
    }

    @Override
    public void onNeighborBlockChange(World world, int x, int y, int z, Block neighbor) {
        if (world.isRemote || mcc$vanilla != null) return;
        int meta = world.getBlockMetadata(x, y, z);
        boolean powered = world.isBlockIndirectlyGettingPowered(x, y, z);
        if (powered || neighbor.canProvidePower()) {
            if (powered && !isFenceGateOpen(meta)) {
                world.setBlockMetadataWithNotify(x, y, z, meta | 4, 2);
                world.playAuxSFXAtEntity(null, 1003, x, y, z, 0);
            } else if (!powered && isFenceGateOpen(meta)) {
                world.setBlockMetadataWithNotify(x, y, z, meta & -5, 2);
                world.playAuxSFXAtEntity(null, 1003, x, y, z, 0);
            }
        }
    }
}
