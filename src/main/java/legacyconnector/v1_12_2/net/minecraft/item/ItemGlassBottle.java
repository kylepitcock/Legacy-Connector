package legacyconnector.v1_12_2.net.minecraft.item;

import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;

/** 1.12.2 glass bottle: filled from water or with dragon's breath (the modern bottle's rules). */
public class ItemGlassBottle extends Item {
    public ItemGlassBottle() {
        setCreativeTab(legacyconnector.v1_12_2.net.minecraft.creativetab.CreativeTabs.BREWING);
    }

    @Override
    public legacyconnector.v1_12_2.net.minecraft.util.ActionResult<ItemStack> onItemRightClick(legacyconnector.v1_12_2.net.minecraft.world.World worldIn,
            EntityPlayer playerIn, legacyconnector.v1_12_2.net.minecraft.util.EnumHand handIn) {
        return mcc$useModern(net.minecraft.world.item.Items.GLASS_BOTTLE, worldIn, playerIn, handIn);
    }

    /** Uses up one bottle for {@code stack}: the filled stack replaces an emptied hand, else goes into the inventory (or drops). */
    protected ItemStack turnBottleIntoItem(ItemStack bottles, EntityPlayer player, ItemStack stack) {
        bottles.shrink(1);
        player.addStat(legacyconnector.v1_12_2.net.minecraft.stats.StatList.getObjectUseStats(this));
        if (bottles.isEmpty()) return stack;
        var p = player.mcc$player();
        if (p != null) {
            var modern = org.kylepitcock.legacyconnector.v1_12_2.bridge.Stacks1122.toModern(stack);
            if (!p.getInventory().add(modern)) p.drop(modern, false);
        }
        return bottles;
    }
}
