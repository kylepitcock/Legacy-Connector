package legacyconnector.v1_12_2.net.minecraft.enchantment;

import java.util.Random;
import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_12_2.net.minecraft.inventory.EntityEquipmentSlot;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;

/** 1.12.2 thorns: a chance (15% per level) to hurt an attacker for 1-4 (or level - 10) damage, wearing the armour. */
public class EnchantmentThorns extends Enchantment {
    public EnchantmentThorns(Enchantment.Rarity rarityIn, EntityEquipmentSlot... slots) {
        super(rarityIn, EnumEnchantmentType.ARMOR_CHEST, slots);
        setName("thorns");
    }

    @Override
    public int getMinEnchantability(int enchantmentLevel) {
        return 10 + 20 * (enchantmentLevel - 1);
    }

    @Override
    public int getMaxEnchantability(int enchantmentLevel) {
        return super.getMinEnchantability(enchantmentLevel) + 50;
    }

    @Override
    public int getMaxLevel() {
        return 3;
    }

    @Override
    public boolean canApply(ItemStack stack) {
        return stack.getItem() instanceof legacyconnector.v1_12_2.net.minecraft.item.ItemArmor || super.canApply(stack);
    }

    @Override
    public void onUserHurt(EntityLivingBase user, Entity attacker, int level) {
        Random random = user.getRNG();
        if (attacker != null && shouldHit(level, random)) {
            attacker.attackEntityFrom(legacyconnector.v1_12_2.net.minecraft.util.DamageSource.causeThornsDamage(user), getDamage(level, random));
        }
    }

    public static boolean shouldHit(int level, Random rnd) {
        return level > 0 && rnd.nextFloat() < 0.15F * level;
    }

    public static int getDamage(int level, Random rnd) {
        return level > 10 ? level - 10 : 1 + rnd.nextInt(4);
    }
}
