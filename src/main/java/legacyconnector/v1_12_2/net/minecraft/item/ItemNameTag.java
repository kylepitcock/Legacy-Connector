package legacyconnector.v1_12_2.net.minecraft.item;

import legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.util.EnumHand;

/** 1.12.2 name tag: a renamed tag used on a mob names it (the mob then never despawns) and is used up. */
public class ItemNameTag extends Item {
    public ItemNameTag() {
        setCreativeTab(legacyconnector.v1_12_2.net.minecraft.creativetab.CreativeTabs.TOOLS);
    }

    @Override
    public boolean itemInteractionForEntity(ItemStack stack, EntityPlayer playerIn, EntityLivingBase target, EnumHand hand) {
        if (!stack.hasDisplayName() || target == null || target instanceof EntityPlayer) return false;
        var modern = target.mcc$entity;
        if (modern == null) return false;
        if (!modern.level().isClientSide && modern.isAlive()) {
            modern.setCustomName(net.minecraft.network.chat.Component.literal(stack.getDisplayName()));
            if (modern instanceof net.minecraft.world.entity.Mob mob) mob.setPersistenceRequired();
            stack.shrink(1);
        }
        return true;
    }
}
