package legacyconnector.v1_7_10.net.minecraft.block;

import java.util.Random;
import legacyconnector.v1_7_10.net.minecraft.block.material.Material;
import legacyconnector.v1_7_10.net.minecraft.init.Blocks;
import legacyconnector.v1_7_10.net.minecraft.init.Items;
import legacyconnector.v1_7_10.net.minecraft.item.Item;
import legacyconnector.v1_7_10.net.minecraft.util.AxisAlignedBB;
import legacyconnector.v1_7_10.net.minecraft.world.IBlockAccess;
import legacyconnector.v1_7_10.net.minecraft.world.World;
import legacyconnector.v1_7_10.net.minecraftforge.common.EnumPlantType;
import legacyconnector.v1_7_10.net.minecraftforge.common.IPlantable;
import legacyconnector.v1_7_10.net.minecraftforge.common.util.ForgeDirection;

/**
 * Sugar cane, ported from 1.7.10: a beach plant that grows up to three blocks tall (metadata counts the random ticks to the next
 * block) and breaks when its support goes; tinted with the grass colour.
 */
public class BlockReed extends Block implements IPlantable {
    public BlockReed(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info) {
        super(info);
    }

    protected BlockReed() {
        super(Material.plants);
        float f = 0.375F;
        setBlockBounds(0.5F - f, 0.0F, 0.5F - f, 0.5F + f, 1.0F, 0.5F + f);
        setTickRandomly(true);
    }

    @Override
    public void updateTick(World world, int x, int y, int z, Random random) {
        if (world.getBlock(x, y - 1, z) != Blocks.reeds && !func_150170_e(world, x, y, z)) return;
        if (!world.isAirBlock(x, y + 1, z)) return;
        int height = 1;
        while (world.getBlock(x, y - height, z) == this) ++height;
        if (height >= 3) return;
        int meta = world.getBlockMetadata(x, y, z);
        if (meta == 15) {
            world.setBlock(x, y + 1, z, this);
            world.setBlockMetadataWithNotify(x, y, z, 0, 4);
        } else {
            world.setBlockMetadataWithNotify(x, y, z, meta + 1, 4);
        }
    }

    @Override
    public boolean canPlaceBlockAt(World world, int x, int y, int z) {
        Block below = world.getBlock(x, y - 1, z);
        return below.canSustainPlant(world, x, y - 1, z, ForgeDirection.UP, this);
    }

    @Override
    public void onNeighborBlockChange(World world, int x, int y, int z, Block neighbor) {
        func_150170_e(world, x, y, z);
    }

    /** Drops the cane when it cannot stay; false then. */
    protected final boolean func_150170_e(World world, int x, int y, int z) {
        if (canBlockStay(world, x, y, z)) return true;
        dropBlockAsItem(world, x, y, z, world.getBlockMetadata(x, y, z), 0);
        world.setBlockToAir(x, y, z);
        return false;
    }

    @Override
    public boolean canBlockStay(World world, int x, int y, int z) {
        return canPlaceBlockAt(world, x, y, z);
    }

    @Override
    public AxisAlignedBB getCollisionBoundingBoxFromPool(World world, int x, int y, int z) {
        return null;
    }

    @Override
    public Item getItemDropped(int meta, Random random, int fortune) {
        return Items.reeds;
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
        return 1;
    }

    @Override
    public Item getItem(World world, int x, int y, int z) {
        return Items.reeds;
    }

    @Override
    public int colorMultiplier(IBlockAccess world, int x, int y, int z) {
        return world.mcc$blockTint(x, y, z, 0);
    }

    @Override
    public EnumPlantType getPlantType(IBlockAccess world, int x, int y, int z) {
        return EnumPlantType.Beach;
    }

    @Override
    public Block getPlant(IBlockAccess world, int x, int y, int z) {
        return this;
    }

    @Override
    public int getPlantMetadata(IBlockAccess world, int x, int y, int z) {
        return world.getBlockMetadata(x, y, z);
    }
}
