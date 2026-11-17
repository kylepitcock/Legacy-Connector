package legacyconnector.v1_6_4.net.minecraft.src.forge;

import legacyconnector.v1_7_10.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;
import legacyconnector.v1_7_10.net.minecraft.util.DamageSource;

/**
 * Forge 3.x's armour with its own damage rules. It IS the shim's Forge armour interface, so the layer's armour path
 * ({@code ISpecialArmor.ArmorProperties.ApplyArmor} on incoming damage) applies it with no second implementation. The parameter type is
 * {@code EntityLivingBase} because the loader names every {@code EntityLiving} of a 1.2.5 jar's signatures that way; the covariant
 * {@code forge.ArmorProperties} return makes the compiler emit the bridge method the shared interface needs.
 */
public interface ISpecialArmor extends legacyconnector.v1_7_10.net.minecraftforge.common.ISpecialArmor {
    @Override
    legacyconnector.v1_6_4.net.minecraft.src.forge.ArmorProperties getProperties(EntityLivingBase player, ItemStack armor, DamageSource source, double damage, int slot);

    @Override
    int getArmorDisplay(EntityPlayer player, ItemStack armor, int slot);

    @Override
    void damageArmor(EntityLivingBase entity, ItemStack stack, DamageSource source, int damage, int slot);
}
