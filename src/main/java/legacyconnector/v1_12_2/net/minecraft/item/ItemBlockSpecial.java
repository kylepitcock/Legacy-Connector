package legacyconnector.v1_12_2.net.minecraft.item;

import legacyconnector.v1_12_2.net.minecraft.block.Block;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.util.EnumActionResult;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import legacyconnector.v1_12_2.net.minecraft.util.EnumHand;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/** An item that places a block without being its block item (sugar cane, cake, repeaters...). */
public class ItemBlockSpecial extends Item {
    private final Block block;

    public ItemBlockSpecial(Block block) {
        this.block = block;
    }

    public Block getBlock() {
        return block;
    }

    @Override
    public EnumActionResult onItemUse(EntityPlayer player, World worldIn, BlockPos pos, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        IBlockState iblockstate = worldIn.getBlockState(pos);
        if (!iblockstate.getBlock().isReplaceable(worldIn, pos)) pos = pos.offset(facing);
        ItemStack itemstack = player.getHeldItem(hand);
        if (!itemstack.isEmpty() && player.canPlayerEdit(pos, facing, itemstack) && worldIn.getBlockState(pos).getBlock().isReplaceable(worldIn, pos)
                && block.canPlaceBlockAt(worldIn, pos)) {
            IBlockState placed = block.getStateForPlacement(worldIn, pos, facing, hitX, hitY, hitZ, 0, player);
            if (worldIn.setBlockState(pos, placed, 11)) {
                IBlockState now = worldIn.getBlockState(pos);
                if (now.getBlock() == block) now.getBlock().onBlockPlacedBy(worldIn, pos, now, player, itemstack);
                itemstack.shrink(1);
            }
            return EnumActionResult.SUCCESS;
        }
        return EnumActionResult.FAIL;
    }
}
