package legacyconnector.v1_12_2.net.minecraft.item;

import legacyconnector.v1_12_2.net.minecraft.creativetab.CreativeTabs;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.util.EnumActionResult;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import legacyconnector.v1_12_2.net.minecraft.util.EnumHand;
import legacyconnector.v1_12_2.net.minecraft.util.NonNullList;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/** 1.12.2 bed item: one item with sixteen colours (the metadata); placing it puts the two-block bed of that colour (the modern bed's). */
public class ItemBed extends Item {
    public ItemBed() {
        setCreativeTab(CreativeTabs.DECORATIONS);
        setMaxDamage(0);
        setHasSubtypes(true);
    }

    @Override
    public EnumActionResult onItemUse(EntityPlayer player, World worldIn, BlockPos pos, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (worldIn.isRemote) return EnumActionResult.SUCCESS;
        if (facing != EnumFacing.UP) return EnumActionResult.FAIL;
        int meta = player.getHeldItem(hand).getMetadata();
        var color = net.minecraft.world.item.DyeColor.byId(EnumDyeColor.byMetadata(meta).getMetadata());
        var bed = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.withDefaultNamespace(color.getName() + "_bed"));
        return mcc$useOnModern(bed, player, worldIn, pos, hand, facing, hitX, hitY, hitZ);
    }

    @Override
    public String getUnlocalizedName(ItemStack stack) {
        return super.getUnlocalizedName() + "." + EnumDyeColor.byMetadata(stack.getMetadata()).getTranslationKey();
    }

    @Override
    public void getSubItems(CreativeTabs tab, NonNullList<ItemStack> items) {
        if (isInCreativeTab(tab)) {
            for (int i = 0; i < 16; ++i) items.add(new ItemStack(this, 1, i));
        }
    }
}
