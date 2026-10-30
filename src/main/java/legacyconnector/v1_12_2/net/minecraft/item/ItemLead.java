package legacyconnector.v1_12_2.net.minecraft.item;

import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.util.EnumActionResult;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import legacyconnector.v1_12_2.net.minecraft.util.EnumHand;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/** 1.12.2 lead: used on a fence, ties the mobs the player leads to it (the modern lead's knot). */
public class ItemLead extends Item {
    public ItemLead() {
        setCreativeTab(legacyconnector.v1_12_2.net.minecraft.creativetab.CreativeTabs.TOOLS);
    }

    @Override
    public EnumActionResult onItemUse(EntityPlayer player, World worldIn, BlockPos pos, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        var level = worldIn.mcc$level();
        if (level == null || !level.getBlockState(pos.mcc$modern()).is(net.minecraft.tags.BlockTags.FENCES)) return EnumActionResult.PASS;
        if (!worldIn.isRemote) attachToFence(player, worldIn, pos);
        return EnumActionResult.SUCCESS;
    }

    /** Ties every mob the player leads within seven blocks to a knot on the fence; true when there was one. */
    public static boolean attachToFence(EntityPlayer player, World worldIn, BlockPos fence) {
        var p = player == null ? null : player.mcc$player();
        var level = worldIn == null ? null : worldIn.mcc$level();
        if (p == null || level == null || fence == null) return false;
        return net.minecraft.world.item.LeadItem.bindPlayerMobs(p, level, fence.mcc$modern()).consumesAction();
    }
}
