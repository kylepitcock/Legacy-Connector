package legacyconnector.v1_12_2.net.minecraft.item;

import legacyconnector.v1_12_2.net.minecraft.block.Block;
import legacyconnector.v1_12_2.net.minecraft.block.BlockSlab;
import legacyconnector.v1_12_2.net.minecraft.block.SoundType;
import legacyconnector.v1_12_2.net.minecraft.block.properties.IProperty;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.util.EnumActionResult;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import legacyconnector.v1_12_2.net.minecraft.util.EnumHand;
import legacyconnector.v1_12_2.net.minecraft.util.SoundCategory;
import legacyconnector.v1_12_2.net.minecraft.util.math.AxisAlignedBB;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/** The item of a slab: placing it on a matching half slab makes the double slab; otherwise it places a half slab. */
public class ItemSlab extends ItemBlock {
    private final BlockSlab singleSlab;
    private final BlockSlab doubleSlab;

    public ItemSlab(Block block, BlockSlab singleSlab, BlockSlab doubleSlab) {
        super(block);
        this.singleSlab = singleSlab;
        this.doubleSlab = doubleSlab;
        setMaxDamage(0);
        setHasSubtypes(true);
    }

    @Override
    public int getMetadata(int damage) {
        return damage;
    }

    @Override
    public String getUnlocalizedName(ItemStack stack) {
        return singleSlab.getUnlocalizedName(stack.getMetadata());
    }

    @Override
    public EnumActionResult onItemUse(EntityPlayer player, World worldIn, BlockPos pos, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        ItemStack itemstack = player.getHeldItem(hand);
        if (itemstack.isEmpty() || !player.canPlayerEdit(pos.offset(facing), facing, itemstack)) return EnumActionResult.FAIL;
        Comparable<?> comparable = singleSlab.getTypeForItem(itemstack);
        IBlockState iblockstate = worldIn.getBlockState(pos);
        if (iblockstate.getBlock() == singleSlab) {
            IProperty<?> iproperty = singleSlab.getVariantProperty();
            Comparable<?> comparable1 = iblockstate.getValue(iproperty);
            BlockSlab.EnumBlockHalf half = iblockstate.getValue(BlockSlab.HALF);
            if ((facing == EnumFacing.UP && half == BlockSlab.EnumBlockHalf.BOTTOM || facing == EnumFacing.DOWN && half == BlockSlab.EnumBlockHalf.TOP)
                    && comparable1 == comparable) {
                place(player, itemstack, worldIn, pos, makeState(iproperty, comparable1));
                return EnumActionResult.SUCCESS;
            }
        }
        return tryPlace(player, itemstack, worldIn, pos.offset(facing), comparable) ? EnumActionResult.SUCCESS
                : super.onItemUse(player, worldIn, pos, hand, facing, hitX, hitY, hitZ);
    }

    private boolean tryPlace(EntityPlayer player, ItemStack stack, World worldIn, BlockPos pos, Object itemSlabType) {
        IBlockState iblockstate = worldIn.getBlockState(pos);
        if (iblockstate.getBlock() == singleSlab) {
            Comparable<?> comparable = iblockstate.getValue(singleSlab.getVariantProperty());
            if (comparable == itemSlabType) {
                place(player, stack, worldIn, pos, makeState(singleSlab.getVariantProperty(), comparable));
                return true;
            }
        }
        return false;
    }

    /** Turns the half slab into the double slab when no entity is in the way. */
    private void place(EntityPlayer player, ItemStack stack, World worldIn, BlockPos pos, IBlockState doubleState) {
        AxisAlignedBB axisalignedbb = doubleState.getCollisionBoundingBox(worldIn, pos);
        if (axisalignedbb != Block.NULL_AABB && worldIn.checkNoEntityCollision(axisalignedbb.offset(pos)) && worldIn.setBlockState(pos, doubleState, 11)) {
            SoundType soundtype = doubleSlab.getSoundType(doubleState, worldIn, pos, player);
            worldIn.playSound(player, pos, soundtype.getPlaceSound(), SoundCategory.BLOCKS, (soundtype.getVolume() + 1.0F) / 2.0F, soundtype.getPitch() * 0.8F);
            stack.shrink(1);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private IBlockState makeState(IProperty property, Comparable value) {
        return doubleSlab.getDefaultState().withProperty(property, value);
    }
}
