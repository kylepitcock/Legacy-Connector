package legacyconnector.v1_7_10.net.minecraft.item;

import legacyconnector.v1_7_10.net.minecraft.creativetab.CreativeTabs;
import legacyconnector.v1_7_10.net.minecraft.entity.passive.EntityPig;
import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/**
 * Carrot on a stick (1.7.10 {@code ItemCarrotOnAStick}): 25 uses, held like a rod; right-clicking while riding a saddled pig boosts it
 * (the modern pig's own boost, which also makes it follow the carrot) at 7 uses a time. It turns into a fishing rod when used up.
 */
public class ItemCarrotOnAStick extends Item {
    public ItemCarrotOnAStick(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info) {
        super(info);
    }

    public ItemCarrotOnAStick() {
        super();
        setCreativeTab(CreativeTabs.tabTransport);
        setMaxStackSize(1);
        setMaxDamage(25);
    }

    @Override
    public boolean isFull3D() {
        return true;
    }

    @Override
    public boolean shouldRotateAroundWhenRendering() {
        return true;
    }

    @Override
    public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
        if (player.isRiding() && legacyconnector.v1_7_10.net.minecraft.entity.Entity.mcc$get$ridingEntity(player) instanceof EntityPig pig && stack.getMaxDamage() - stack.getItemDamage() >= 7
                && pig.mcc$entity() instanceof net.minecraft.world.entity.animal.Pig modernPig && modernPig.isSaddled() && modernPig.boost()) {
            stack.damageItem(7, player);
            if (ItemStack.mcc$get$stackSize(stack) == 0) {
                ItemStack rod = new ItemStack(legacyconnector.v1_7_10.net.minecraft.init.Items.fishing_rod);
                ItemStack.mcc$set$stackTagCompound(rod, ItemStack.mcc$get$stackTagCompound(stack));
                return rod;
            }
        }
        return stack;
    }
}
