package legacyconnector.v1_12_2.baubles.api;

import legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;

/** Baubles 1.12's wearable item: implemented by the item, called by the Curios slots it is worn in. */
public interface IBauble {
    BaubleType getBaubleType(ItemStack itemstack);

    default void onWornTick(ItemStack itemstack, EntityLivingBase player) {
    }

    default void onEquipped(ItemStack itemstack, EntityLivingBase player) {
    }

    default void onUnequipped(ItemStack itemstack, EntityLivingBase player) {
    }

    default boolean canEquip(ItemStack itemstack, EntityLivingBase player) {
        return true;
    }

    default boolean canUnequip(ItemStack itemstack, EntityLivingBase player) {
        return true;
    }

    default boolean willAutoSync(ItemStack itemstack, EntityLivingBase player) {
        return false;
    }
}
