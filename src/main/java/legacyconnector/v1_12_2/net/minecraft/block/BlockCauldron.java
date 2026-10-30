package legacyconnector.v1_12_2.net.minecraft.block;

import java.util.List;
import java.util.Random;
import legacyconnector.v1_12_2.net.minecraft.block.material.MapColor;
import legacyconnector.v1_12_2.net.minecraft.block.material.Material;
import legacyconnector.v1_12_2.net.minecraft.block.properties.PropertyInteger;
import legacyconnector.v1_12_2.net.minecraft.block.state.BlockFaceShape;
import legacyconnector.v1_12_2.net.minecraft.block.state.BlockStateContainer;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.init.Items;
import legacyconnector.v1_12_2.net.minecraft.item.Item;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import legacyconnector.v1_12_2.net.minecraft.util.EnumHand;
import legacyconnector.v1_12_2.net.minecraft.util.math.AxisAlignedBB;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.IBlockAccess;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/**
 * 1.12.2 cauldron: holds three levels of water (the metadata), filled by a water bucket or rain, emptied by bottles and a bucket, and
 * puts out burning entities that stand in it. A vanilla cauldron's level is the modern water cauldron's.
 */
public class BlockCauldron extends Block {
    public static final PropertyInteger LEVEL = PropertyInteger.create("level", 0, 3);
    protected static final AxisAlignedBB AABB_LEGS = new AxisAlignedBB(0.0D, 0.0D, 0.0D, 1.0D, 0.3125D, 1.0D);
    protected static final AxisAlignedBB AABB_WALL_NORTH = new AxisAlignedBB(0.0D, 0.0D, 0.0D, 1.0D, 1.0D, 0.125D);
    protected static final AxisAlignedBB AABB_WALL_SOUTH = new AxisAlignedBB(0.0D, 0.0D, 0.875D, 1.0D, 1.0D, 1.0D);
    protected static final AxisAlignedBB AABB_WALL_EAST = new AxisAlignedBB(0.875D, 0.0D, 0.0D, 1.0D, 1.0D, 1.0D);
    protected static final AxisAlignedBB AABB_WALL_WEST = new AxisAlignedBB(0.0D, 0.0D, 0.0D, 0.125D, 1.0D, 1.0D);

    public BlockCauldron() {
        super(Material.IRON, MapColor.STONE);
        setDefaultState(blockState.getBaseState().withProperty(LEVEL, 0));
    }

    @Override
    public void addCollisionBoxToList(IBlockState state, World worldIn, BlockPos pos, AxisAlignedBB entityBox, List<AxisAlignedBB> collidingBoxes,
            Entity entityIn, boolean isActualState) {
        addCollisionBoxToList(pos, entityBox, collidingBoxes, AABB_LEGS);
        addCollisionBoxToList(pos, entityBox, collidingBoxes, AABB_WALL_WEST);
        addCollisionBoxToList(pos, entityBox, collidingBoxes, AABB_WALL_NORTH);
        addCollisionBoxToList(pos, entityBox, collidingBoxes, AABB_WALL_EAST);
        addCollisionBoxToList(pos, entityBox, collidingBoxes, AABB_WALL_SOUTH);
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        return FULL_BLOCK_AABB;
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return false;
    }

    /** Puts out a burning entity whose feet are below the water surface (and takes a level). */
    @Override
    public void onEntityCollision(World worldIn, BlockPos pos, IBlockState state, Entity entityIn) {
        int level = mcc$level(worldIn, pos, state);
        float surface = pos.getY() + (6.0F + 3 * level) / 16.0F;
        if (!worldIn.isRemote && entityIn.isBurning() && level > 0 && entityIn.getEntityBoundingBox().minY <= surface) {
            entityIn.extinguish();
            setWaterLevel(worldIn, pos, state, level - 1);
        }
    }

    /** A water bucket fills it, a bucket takes the water, a bottle takes a level. */
    @Override
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn, EnumHand hand, EnumFacing facing, float hitX,
            float hitY, float hitZ) {
        ItemStack held = playerIn.getHeldItem(hand);
        if (held.isEmpty()) return true;
        int level = mcc$level(worldIn, pos, state);
        Item item = held.getItem();
        if (item == Items.WATER_BUCKET) {
            if (level < 3 && !worldIn.isRemote) {
                if (!playerIn.isCreative()) playerIn.setHeldItem(hand, new ItemStack(Items.BUCKET));
                setWaterLevel(worldIn, pos, state, 3);
            }
            return true;
        }
        if (item == Items.BUCKET) {
            if (level == 3 && !worldIn.isRemote) {
                if (!playerIn.isCreative()) {
                    held.shrink(1);
                    if (held.isEmpty()) playerIn.setHeldItem(hand, new ItemStack(Items.WATER_BUCKET));
                    else if (!EntityPlayer.mcc$get$inventory(playerIn).addItemStackToInventory(new ItemStack(Items.WATER_BUCKET))) playerIn.dropItem(new ItemStack(Items.WATER_BUCKET), false);
                }
                setWaterLevel(worldIn, pos, state, 0);
            }
            return true;
        }
        if (item == Items.GLASS_BOTTLE) {
            if (level > 0 && !worldIn.isRemote) {
                if (!playerIn.isCreative()) {
                    ItemStack water = legacyconnector.v1_12_2.net.minecraft.potion.PotionUtils.addPotionToItemStack(new ItemStack(Items.POTIONITEM),
                            legacyconnector.v1_12_2.net.minecraft.potion.PotionType.mcc$vanilla("water"));
                    held.shrink(1);
                    if (held.isEmpty()) playerIn.setHeldItem(hand, water);
                    else if (!EntityPlayer.mcc$get$inventory(playerIn).addItemStackToInventory(water)) playerIn.dropItem(water, false);
                }
                setWaterLevel(worldIn, pos, state, level - 1);
            }
            return true;
        }
        return false;
    }

    /** Sets the water level (0 empties it): the modern cauldron or water cauldron for a vanilla one, the LEVEL property otherwise. */
    public void setWaterLevel(World worldIn, BlockPos pos, IBlockState state, int level) {
        level = Math.max(0, Math.min(3, level));
        if (isVanilla() && worldIn.mcc$level() != null) {
            var mp = org.kylepitcock.legacyconnector.v1_12_2.bridge.Inventories1122.pos(pos);
            var modern = level == 0 ? net.minecraft.world.level.block.Blocks.CAULDRON.defaultBlockState()
                    : net.minecraft.world.level.block.Blocks.WATER_CAULDRON.defaultBlockState().setValue(net.minecraft.world.level.block.LayeredCauldronBlock.LEVEL, level);
            worldIn.mcc$level().setBlock(mp, modern, 2);
        } else {
            worldIn.setBlockState(pos, state.withProperty(LEVEL, level), 2);
        }
        worldIn.updateComparatorOutputLevel(pos, this);
    }

    /** Rain fills it a level now and then. */
    public void fillWithRain(World worldIn, BlockPos pos) {
        if (worldIn.rand.nextInt(20) != 1) return;
        float temp = worldIn.getBiome(pos).getTemperature(pos);
        if (temp < 0.15F) return;
        IBlockState state = worldIn.getBlockState(pos);
        int level = mcc$level(worldIn, pos, state);
        if (level < 3) setWaterLevel(worldIn, pos, state, level + 1);
    }

    private boolean isVanilla() {
        return mcc$modern instanceof net.minecraft.world.level.block.Block b && !(b instanceof org.kylepitcock.legacyconnector.v1_12_2.adapter.Legacy1122Block);
    }

    /** The water level there: the modern water cauldron's for a vanilla cauldron. */
    private int mcc$level(World worldIn, BlockPos pos, IBlockState state) {
        if (isVanilla() && worldIn.mcc$level() != null) {
            var modern = worldIn.mcc$level().getBlockState(org.kylepitcock.legacyconnector.v1_12_2.bridge.Inventories1122.pos(pos));
            return modern.hasProperty(net.minecraft.world.level.block.LayeredCauldronBlock.LEVEL) && modern.is(net.minecraft.world.level.block.Blocks.WATER_CAULDRON)
                    ? modern.getValue(net.minecraft.world.level.block.LayeredCauldronBlock.LEVEL) : 0;
        }
        return state.getPropertyKeys().contains(LEVEL) ? state.getValue(LEVEL) : 0;
    }

    @Override
    public Item getItemDropped(IBlockState state, Random rand, int fortune) {
        return Items.CAULDRON;
    }

    @Override
    public ItemStack getItem(World worldIn, BlockPos pos, IBlockState state) {
        return new ItemStack(Items.CAULDRON);
    }

    @Override
    public boolean hasComparatorInputOverride(IBlockState state) {
        return true;
    }

    @Override
    public int getComparatorInputOverride(IBlockState blockState, World worldIn, BlockPos pos) {
        return mcc$level(worldIn, pos, blockState);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState().withProperty(LEVEL, meta & 3);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(LEVEL);
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, LEVEL);
    }

    @Override
    public boolean isPassable(IBlockAccess worldIn, BlockPos pos) {
        return true;
    }

    @Override
    public BlockFaceShape getBlockFaceShape(IBlockAccess worldIn, IBlockState state, BlockPos pos, EnumFacing face) {
        if (face == EnumFacing.UP) return BlockFaceShape.BOWL;
        return face == EnumFacing.DOWN ? BlockFaceShape.UNDEFINED : BlockFaceShape.SOLID;
    }
}
