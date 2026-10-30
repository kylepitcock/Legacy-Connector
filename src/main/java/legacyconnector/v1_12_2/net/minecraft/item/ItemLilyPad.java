package legacyconnector.v1_12_2.net.minecraft.item;

import legacyconnector.v1_12_2.net.minecraft.block.Block;
import legacyconnector.v1_12_2.net.minecraft.block.BlockLiquid;
import legacyconnector.v1_12_2.net.minecraft.block.material.Material;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.util.ActionResult;
import legacyconnector.v1_12_2.net.minecraft.util.EnumActionResult;
import legacyconnector.v1_12_2.net.minecraft.util.EnumHand;
import legacyconnector.v1_12_2.net.minecraft.util.SoundCategory;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.util.math.RayTraceResult;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/** 1.12.2 lily pad item: aimed at still water (or ice), places its block on top of it. */
public class ItemLilyPad extends ItemColored {
    public ItemLilyPad(Block block) {
        super(block, false);
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World worldIn, EntityPlayer playerIn, EnumHand handIn) {
        ItemStack itemstack = playerIn.getHeldItem(handIn);
        RayTraceResult hit = rayTrace(worldIn, playerIn, true);
        if (hit == null) return new ActionResult<>(EnumActionResult.PASS, itemstack);
        if (hit.typeOfHit == RayTraceResult.Type.BLOCK) {
            BlockPos blockpos = hit.getBlockPos();
            if (!worldIn.isBlockModifiable(playerIn, blockpos) || !playerIn.canPlayerEdit(blockpos.offset(hit.sideHit), hit.sideHit, itemstack)) {
                return new ActionResult<>(EnumActionResult.FAIL, itemstack);
            }
            BlockPos above = blockpos.up();
            IBlockState state = worldIn.getBlockState(blockpos);
            boolean stillWater = state.getMaterial() == Material.WATER && state.getPropertyKeys().contains(BlockLiquid.LEVEL)
                    && state.getValue(BlockLiquid.LEVEL) == 0;
            if ((stillWater || state.getMaterial() == Material.ICE) && worldIn.isAirBlock(above)) {
                worldIn.setBlockState(above, block.getDefaultState(), 11);
                if (!playerIn.isCreative()) itemstack.shrink(1);
                playerIn.addStat(legacyconnector.v1_12_2.net.minecraft.stats.StatList.getObjectUseStats(this));
                worldIn.playSound(playerIn, blockpos, legacyconnector.v1_12_2.net.minecraft.init.SoundEvents.BLOCK_WATERLILY_PLACE, SoundCategory.BLOCKS,
                        1.0F, 1.0F);
                return new ActionResult<>(EnumActionResult.SUCCESS, itemstack);
            }
        }
        return new ActionResult<>(EnumActionResult.FAIL, itemstack);
    }
}
