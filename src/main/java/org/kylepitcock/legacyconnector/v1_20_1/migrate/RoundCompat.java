package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.Enchantments;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

/** Helpers of 1.20.1 calls that 1.21.1 dropped (ATM 9 ranking round of 2026-11-08). */
public final class RoundCompat {
    private RoundCompat() {}

    /** {@code ItemStack.equals(ItemStack, boolean limitTags)}: same item, count and (components of) tags. */
    public static boolean stackEquals(ItemStack a, ItemStack b, boolean limitTags) {
        return ItemStack.matches(a, b);
    }

    /** {@code BuildCreativeModeTabContentsEvent.getEntries()}: the entries of the tab itself (the parent tab list), where mods put and reorder stacks. */
    public static net.neoforged.neoforge.common.util.MutableHashedLinkedMap<ItemStack, net.minecraft.world.item.CreativeModeTab.TabVisibility> tabEntries(
            BuildCreativeModeTabContentsEvent event) {
        return new LegacyTabEntries(event);
    }

    /** {@code IShearable.isShearable(ItemStack, Level, BlockPos)}: 1.21.1 also passes the shearing player (null: not known). */
    public static boolean isShearable(net.neoforged.neoforge.common.IShearable target, ItemStack stack, net.minecraft.world.level.Level level,
            net.minecraft.core.BlockPos pos) {
        return target.isShearable(null, stack, level, pos);
    }

    public static java.util.List<ItemStack> onSheared(net.neoforged.neoforge.common.IShearable target, net.minecraft.world.entity.player.Player player,
            ItemStack stack, net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos, int fortune) {
        return target.onSheared(player, stack, level, pos);
    }

    /** The fortune level the 1.20.1 {@code onSheared} got as its last argument: read from the shears (the 1.21.1 method has no such argument). */
    public static int shearFortune(ItemStack shears) {
        return HolderCompat.stackEnchantmentLevel(shears, LegacyEnchantments.legacy(ench(Enchantments.FORTUNE)));
    }

    /** {@code BuiltInRegistries.POTION} / {@code PAINTING_VARIANT} were typed {@code DefaultedRegistry}; 1.21.1 types them {@code Registry} (the object still is one). */
    public static net.minecraft.core.DefaultedRegistry<?> defaulted(net.minecraft.core.Registry<?> registry) {
        return (net.minecraft.core.DefaultedRegistry<?>) registry;
    }

    private static Holder<Enchantment> ench(ResourceKey<Enchantment> key) {
        return MigrateContext.registries().registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(key);
    }

    public static boolean hasFrostWalker(LivingEntity entity) {
        return EnchantmentHelper.getEnchantmentLevel(ench(Enchantments.FROST_WALKER), entity) > 0;
    }

    public static int getFireAspect(LivingEntity entity) {
        return EnchantmentHelper.getEnchantmentLevel(ench(Enchantments.FIRE_ASPECT), entity);
    }

    public static int getKnockbackBonus(LivingEntity entity) {
        return EnchantmentHelper.getEnchantmentLevel(ench(Enchantments.KNOCKBACK), entity);
    }

    public static boolean hasBindingCurse(ItemStack stack) {
        return EnchantmentHelper.getItemEnchantmentLevel(ench(Enchantments.BINDING_CURSE), stack) > 0;
    }

    public static boolean hasVanishingCurse(ItemStack stack) {
        return EnchantmentHelper.getItemEnchantmentLevel(ench(Enchantments.VANISHING_CURSE), stack) > 0;
    }

    /** {@code EnchantmentHelper.getTagEnchantmentLevel(Enchantment, ItemStack)}: the level stored on the stack. */
    public static int getTagEnchantmentLevel(LegacyEnchantment e, ItemStack stack) {
        return HolderCompat.stackEnchantmentLevel(stack, e);
    }

    /** {@code EnchantmentInstance.enchantment} (a holder in 1.21.1). */
    public static LegacyEnchantment instanceEnchantment(EnchantmentInstance instance) {
        return LegacyEnchantments.legacy(instance.enchantment);
    }

    /** {@code EnchantmentHelper.deserializeEnchantments(ListTag)}: entries {@code {id:"ns:name", lvl:N}} as a map from enchantment to level. */
    public static Map<LegacyEnchantment, Integer> deserializeEnchantments(ListTag list) {
        Map<LegacyEnchantment, Integer> out = new LinkedHashMap<>();
        for (Tag t : list) {
            if (!(t instanceof CompoundTag c)) continue;
            ResourceLocation id = ResourceLocation.tryParse(c.getString("id"));
            if (id == null) continue;
            var holder = MigrateContext.registries().registryOrThrow(Registries.ENCHANTMENT).getHolder(ResourceKey.create(Registries.ENCHANTMENT, id));
            if (holder.isPresent()) out.put(LegacyEnchantments.legacy(holder.get()), (int) c.getShort("lvl"));
        }
        return out;
    }
}
