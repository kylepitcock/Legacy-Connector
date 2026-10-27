package legacyconnector.v1_12_2.net.minecraft.world.storage.loot.conditions;

import java.util.Random;
import legacyconnector.v1_12_2.net.minecraft.world.storage.loot.LootContext;

/** 1.12.2 loot condition: a chance raised per level of Looting on the killer's weapon. */
public class RandomChanceWithLooting implements LootCondition {
    private final float chance;
    private final float lootingMultiplier;

    public RandomChanceWithLooting(float chance, float lootingMultiplier) {
        this.chance = chance;
        this.lootingMultiplier = lootingMultiplier;
    }

    @Override
    public boolean testCondition(Random rand, LootContext context) {
        int looting = 0;
        var killer = context.getKiller();
        if (killer != null && killer.mcc$entity instanceof net.minecraft.world.entity.LivingEntity living) {
            var level = living.level();
            var enchantment = level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT)
                    .getHolder(net.minecraft.world.item.enchantment.Enchantments.LOOTING);
            if (enchantment.isPresent()) looting = net.minecraft.world.item.enchantment.EnchantmentHelper.getEnchantmentLevel(enchantment.get(), living);
        }
        return rand.nextFloat() < chance + looting * lootingMultiplier;
    }
}
