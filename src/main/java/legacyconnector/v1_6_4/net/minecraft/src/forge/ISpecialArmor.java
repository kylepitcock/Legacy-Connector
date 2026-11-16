package legacyconnector.v1_6_4.net.minecraft.src.forge;

/** Forge 3.x's armour with its own damage reduction (interface only; the layer applies the game's armour calculation). */
public interface ISpecialArmor {
    ArmorProperties getProperties(legacyconnector.v1_7_10.net.minecraft.entity.EntityLiving player, legacyconnector.v1_7_10.net.minecraft.item.ItemStack armor, legacyconnector.v1_7_10.net.minecraft.util.DamageSource source, double damage, int slot);

    int getArmorDisplay(legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer player, legacyconnector.v1_7_10.net.minecraft.item.ItemStack armor, int slot);

    void damageArmor(legacyconnector.v1_7_10.net.minecraft.entity.EntityLiving entity, legacyconnector.v1_7_10.net.minecraft.item.ItemStack stack, legacyconnector.v1_7_10.net.minecraft.util.DamageSource source, int damage, int slot);
}
