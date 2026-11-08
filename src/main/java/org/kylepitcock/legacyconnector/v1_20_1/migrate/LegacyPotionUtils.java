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

    public static int getColor(java.util.Collection<MobEffectInstance> effects) {
        return PotionContents.getColor(effects);
    }

    public static int getColor(ItemStack stack) {
        return stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY).getColor();
    }

    public static ItemStack setCustomEffects(ItemStack stack, java.util.Collection<MobEffectInstance> effects) {
        PotionContents old = stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
        stack.set(DataComponents.POTION_CONTENTS, new PotionContents(old.potion(), old.customColor(), List.copyOf(effects)));
        return stack;
    }

    public static int getColor(Potion potion) {
        return PotionContents.getColor(potion.getEffects());
    }

    /** {@code PotionUtils.getPotion(CompoundTag)}: the {@code Potion} id of an item tag, water when absent. */
    public static Potion getPotion(net.minecraft.nbt.CompoundTag tag) {
        if (tag != null && tag.contains("Potion", net.minecraft.nbt.Tag.TAG_STRING)) {
            net.minecraft.resources.ResourceLocation id = net.minecraft.resources.ResourceLocation.tryParse(tag.getString("Potion"));
            if (id != null && net.minecraft.core.registries.BuiltInRegistries.POTION.containsKey(id)) return net.minecraft.core.registries.BuiltInRegistries.POTION.get(id);
        }
        return Potions.WATER.value();
    }

    /** {@code PotionUtils.getCustomEffects(CompoundTag)}: the {@code CustomPotionEffects} list of an item tag. */
    public static List<MobEffectInstance> getCustomEffects(net.minecraft.nbt.CompoundTag tag) {
        List<MobEffectInstance> out = new java.util.ArrayList<>();
        if (tag != null && tag.contains("CustomPotionEffects", net.minecraft.nbt.Tag.TAG_LIST)) {
            var ops = MigrateContext.provider().createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE);
            for (net.minecraft.nbt.Tag t : tag.getList("CustomPotionEffects", net.minecraft.nbt.Tag.TAG_COMPOUND)) {
                MobEffectInstance.CODEC.parse(ops, t).result().ifPresent(out::add);
            }
        }
        return out;
    }

    public static List<MobEffectInstance> getAllEffects(net.minecraft.nbt.CompoundTag tag) {
        List<MobEffectInstance> out = new java.util.ArrayList<>(getPotion(tag).getEffects());
        out.addAll(getCustomEffects(tag));
        return out;
    }

    public static void addPotionTooltip(ItemStack stack, List<net.minecraft.network.chat.Component> lines, float durationFactor) {
        PotionContents.addPotionTooltip(getMobEffects(stack), lines::add, durationFactor, 20.0f);
    }
}
