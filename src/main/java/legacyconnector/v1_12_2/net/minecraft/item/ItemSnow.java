package legacyconnector.v1_12_2.net.minecraft.item;

import legacyconnector.v1_12_2.net.minecraft.block.Block;
import legacyconnector.v1_12_2.net.minecraft.block.BlockSnow;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.util.EnumActionResult;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import legacyconnector.v1_12_2.net.minecraft.util.EnumHand;
import legacyconnector.v1_12_2.net.minecraft.util.SoundCategory;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/** 1.12.2 snow layer item: used on its own block, adds a layer (up to eight); otherwise places the block. */
public class ItemSnow extends ItemBlock {
    public ItemSnow(Block block) {
        super(block);
        setMaxDamage(0);
        setHasSubtypes(true);
    }

    @Override
    public EnumActionResult onItemUse(EntityPlayer player, World worldIn, BlockPos pos, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        ItemStack itemstack = player.getHeldItem(hand);
        if (itemstack.isEmpty() || !player.canPlayerEdit(pos, facing, itemstack)) return EnumActionResult.FAIL;
        IBlockState state = worldIn.getBlockState(pos);
        Block clicked = state.getBlock();
        BlockPos target = pos;
        if ((facing != EnumFacing.UP || clicked != block) && !clicked.isReplaceable(worldIn, pos)) {
            target = pos.offset(facing);
            state = worldIn.getBlockState(target);
            clicked = state.getBlock();
        }
        if (clicked == block && state.getPropertyKeys().contains(BlockSnow.LAYERS)) {
            int layers = state.getValue(BlockSnow.LAYERS);
            if (layers < 8) {
                IBlockState more = state.withProperty(BlockSnow.LAYERS, layers + 1);
                var bb = more.getCollisionBoundingBox(worldIn, target);
                if (bb != Block.NULL_AABB && worldIn.checkNoEntityCollision(bb.offset(target)) && worldIn.setBlockState(target, more, 10)) {
                    var sound = block.getSoundType(more, worldIn, target, player);
                    worldIn.playSound(player, target, sound.getPlaceSound(), SoundCategory.BLOCKS, (sound.getVolume() + 1.0F) / 2.0F, sound.getPitch() * 0.8F);
                    itemstack.shrink(1);
                    return EnumActionResult.SUCCESS;
                }
            }
        }
        return super.onItemUse(player, worldIn, pos, hand, facing, hitX, hitY, hitZ);
    }

    @Override
    public int getMetadata(int damage) {
        return damage;
    }

    /** Forge: a layer can be added to a snow block that is not full yet. */
    @Override
    public boolean canPlaceBlockOnSide(World world, BlockPos pos, EnumFacing side, EntityPlayer player, ItemStack stack) {
        IBlockState state = world.getBlockState(pos);
        return state.getBlock() == block && state.getPropertyKeys().contains(BlockSnow.LAYERS) && state.getValue(BlockSnow.LAYERS) <= 7
                || super.canPlaceBlockOnSide(world, pos, side, player, stack);
    }
}
