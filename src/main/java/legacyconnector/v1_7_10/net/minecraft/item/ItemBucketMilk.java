package legacyconnector.v1_7_10.net.minecraft.item;

import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraft.entity.player.PlayerCapabilities;
import legacyconnector.v1_7_10.net.minecraft.init.Items;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/** The milk bucket (1.7.10 {@code ItemBucketMilk}): drunk over 32 ticks, it cures every potion effect and leaves an empty bucket. */
public class ItemBucketMilk extends Item {
    public ItemBucketMilk(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info) {
        super(info);
    }

    public ItemBucketMilk() {
        super();
        setMaxStackSize(1);
        setCreativeTab(legacyconnector.v1_7_10.net.minecraft.creativetab.CreativeTabs.tabMisc);
    }

    @Override
    public ItemStack onEaten(ItemStack stack, World world, EntityPlayer player) {
        if (mcc$vanilla != null) return super.onEaten(stack, world, player);
        if (!PlayerCapabilities.mcc$get$isCreativeMode(player.capabilities)) stack.mcc$setCount(stack.mcc$count() - 1);
        if (!world.isRemote) player.clearActivePotions();
        return stack.mcc$count() <= 0 ? new ItemStack(Items.bucket) : stack;
    }

    @Override
    public int getMaxItemUseDuration(ItemStack stack) {
        return 32;
    }

    @Override
    public EnumAction getItemUseAction(ItemStack stack) {
        return EnumAction.drink;
    }

    @Override
    public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
        if (mcc$vanilla != null) return super.onItemRightClick(stack, world, player);
        player.setItemInUse(stack, getMaxItemUseDuration(stack));
        return stack;
    }
}
