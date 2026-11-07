package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;

/** Stands in for 1.20.1's {@code PotionUtils} (potions live in the {@code POTION_CONTENTS} component now). */
public final class LegacyPotionUtils {
    private LegacyPotionUtils() {}

    public static Potion getPotion(ItemStack stack) {
        return stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY).potion().map(h -> h.value()).orElse(Potions.WATER.value());
    }

    public static ItemStack setPotion(ItemStack stack, Potion potion) {
        PotionContents old = stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
        stack.set(DataComponents.POTION_CONTENTS, new PotionContents(java.util.Optional.of(net.minecraft.core.registries.BuiltInRegistries.POTION.wrapAsHolder(potion)),
                old.customColor(), old.customEffects()));
        return stack;
    }

    public static List<MobEffectInstance> getMobEffects(ItemStack stack) {
        List<MobEffectInstance> out = new java.util.ArrayList<>();
        stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY).getAllEffects().forEach(out::add);
        return out;
    }

    public static List<MobEffectInstance> getCustomEffects(ItemStack stack) {
        return stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY).customEffects();
    }

    public static int getColor(ItemStack stack) {
        return stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY).getColor();
    }
}
