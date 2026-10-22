package legacyconnector.v1_7_10.baubles.api;

import legacyconnector.v1_7_10.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;

/** Baubles 1.0 item contract. Items implementing it are registered as Curios curios (see CuriosBaubles). */
public interface IBauble {
    BaubleType getBaubleType(ItemStack itemstack);

    void onWornTick(ItemStack itemstack, EntityLivingBase player);

    void onEquipped(ItemStack itemstack, EntityLivingBase player);

    void onUnequipped(ItemStack itemstack, EntityLivingBase player);

    boolean canEquip(ItemStack itemstack, EntityLivingBase player);

    boolean canUnequip(ItemStack itemstack, EntityLivingBase player);
}
