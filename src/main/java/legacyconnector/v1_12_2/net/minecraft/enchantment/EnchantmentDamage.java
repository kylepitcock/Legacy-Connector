package legacyconnector.v1_12_2.net.minecraft.enchantment;

import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_12_2.net.minecraft.entity.EnumCreatureAttribute;
import legacyconnector.v1_12_2.net.minecraft.inventory.EntityEquipmentSlot;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;

/** 1.12.2 damage enchantments: sharpness (0, every creature), smite (1, the undead) and bane of arthropods (2, which also slows them). */
public class EnchantmentDamage extends Enchantment {
    private static final String[] DAMAGE_NAMES = {"all", "undead", "arthropods"};
    private static final int[] MIN_COST = {1, 5, 5};
    private static final int[] LEVEL_COST = {11, 8, 8};
    private static final int[] LEVEL_COST_SPAN = {20, 20, 20};
    public final int damageType;

    public EnchantmentDamage(Enchantment.Rarity rarityIn, int damageTypeIn, EntityEquipmentSlot... slots) {
        super(rarityIn, EnumEnchantmentType.WEAPON, slots);
        this.damageType = damageTypeIn;
    }

    @Override
    public int getMinEnchantability(int enchantmentLevel) {
        return MIN_COST[damageType] + (enchantmentLevel - 1) * LEVEL_COST[damageType];
    }

    @Override
    public int getMaxEnchantability(int enchantmentLevel) {
        return getMinEnchantability(enchantmentLevel) + LEVEL_COST_SPAN[damageType];
    }

    @Override
    public int getMaxLevel() {
        return 5;
    }

    @Override
    public float calcDamageByCreature(int level, EnumCreatureAttribute creatureType) {
        if (damageType == 0) return 1.0F + Math.max(0, level - 1) * 0.5F;
        if (damageType == 1 && creatureType == EnumCreatureAttribute.UNDEAD) return level * 2.5F;
        return damageType == 2 && creatureType == EnumCreatureAttribute.ARTHROPOD ? level * 2.5F : 0.0F;
    }

    @Override
    public String getName() {
        return "enchantment.damage." + DAMAGE_NAMES[damageType];
    }

    @Override
    protected boolean canApplyTogether(Enchantment ench) {
        return !(ench instanceof EnchantmentDamage);
    }

    @Override
    public boolean canApply(ItemStack stack) {
        return stack.getItem() instanceof legacyconnector.v1_12_2.net.minecraft.item.ItemAxe || super.canApply(stack);
    }

    @Override
    public void onEntityDamaged(EntityLivingBase user, Entity target, int level) {
        if (target instanceof EntityLivingBase living && damageType == 2 && living.getCreatureAttribute() == EnumCreatureAttribute.ARTHROPOD) {
            int ticks = 20 + user.getRNG().nextInt(10 * level);
            living.addPotionEffect(new legacyconnector.v1_12_2.net.minecraft.potion.PotionEffect(legacyconnector.v1_12_2.net.minecraft.init.MobEffects.SLOWNESS, ticks, 3));
        }
    }
}
