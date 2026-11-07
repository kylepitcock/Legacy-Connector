package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;

/**
 * Stands in for the 1.20.1 {@code Enchantment} class in translated mods. In 1.21.1 an enchantment is a final record read from a data pack, so a mod's
 * subclass (cost methods, compatibility, category) cannot link. The subclass extends this class instead; {@link LegacyEnchantments} registers it in a registry of
 * its own and writes the data pack entry (costs, supported items, exclusive set) from its methods, so the enchantment exists in the game under the mod's id and
 * the mod's code finds its level on stacks through {@link #holder()}. Hooks that vanilla 1.21.1 runs through effect components ({@code doPostAttack}...) are not
 * run. For the vanilla enchantments this is a thin wrapper around the data-driven entry ({@link #of(Holder)}).
 */
public class LegacyEnchantment {
    /** 1.20.1's rarity (a weight in the enchanting table). */
    public enum Rarity {
        COMMON(10),
        UNCOMMON(5),
        RARE(2),
        VERY_RARE(1);

        private final int weight;

        Rarity(int weight) {
            this.weight = weight;
        }

        public int getWeight() {
            return weight;
        }
    }

    private final Rarity rarity;
    public final LegacyEnchantmentCategory category;
    private final EquipmentSlot[] slots;
    private String descriptionId;
    private Holder<Enchantment> vanilla;

    protected LegacyEnchantment(Rarity rarity, LegacyEnchantmentCategory category, EquipmentSlot[] slots) {
        this.rarity = rarity;
        this.category = category;
        this.slots = slots;
    }

    /** Wrapper of an entry of the data-driven registry. */
    public static LegacyEnchantment of(Holder<Enchantment> holder) {
        LegacyEnchantment e = new LegacyEnchantment(Rarity.COMMON, null, new EquipmentSlot[0]) {
            @Override
            public int getMaxLevel() {
                return holder.value().getMaxLevel();
            }

            @Override
            public boolean canEnchant(ItemStack stack) {
                return stack.is(holder.value().definition().supportedItems());
            }

            @Override
            public boolean isCurse() {
                return holder.is(net.minecraft.tags.EnchantmentTags.CURSE);
            }

            @Override
            public int getMinCost(int level) {
                return holder.value().getMinCost(level);
            }

            @Override
            public int getMaxCost(int level) {
                return holder.value().getMaxCost(level);
            }

            @Override
            public boolean isTreasureOnly() {
                return holder.is(net.minecraft.tags.EnchantmentTags.TREASURE);
            }

            @Override
            public String getDescriptionId() {
                return Util.makeDescriptionId("enchantment", holder.unwrapKey().map(ResourceKey::location).orElse(null));
            }

            @Override
            public boolean equals(Object o) {
                return o instanceof LegacyEnchantment other && holder.equals(other.vanilla);
            }

            @Override
            public int hashCode() {
                return holder.hashCode();
            }
        };
        e.vanilla = holder;
        return e;
    }

    // ------------------------------------------------------------------ the 1.20.1 surface

    public Rarity getRarity() {
        return rarity;
    }

    public EquipmentSlot[] mcc$slots() {
        return slots;
    }

    public Map<EquipmentSlot, ItemStack> getSlotItems(LivingEntity entity) {
        Map<EquipmentSlot, ItemStack> map = new java.util.EnumMap<>(EquipmentSlot.class);
        for (EquipmentSlot slot : slots) {
            ItemStack stack = entity.getItemBySlot(slot);
            if (!stack.isEmpty()) map.put(slot, stack);
        }
        return map;
    }

    public int getMinLevel() {
        return 1;
    }

    public int getMaxLevel() {
        return 1;
    }

    public int getMinCost(int level) {
        return 1 + level * 10;
    }

    public int getMaxCost(int level) {
        return getMinCost(level) + 5;
    }

    public int getDamageProtection(int level, DamageSource source) {
        return 0;
    }

    public float getDamageBonus(int level, LegacyMobType type) {
        return 0.0F;
    }

    public final boolean isCompatibleWith(LegacyEnchantment other) {
        return checkCompatibility(other) && other.checkCompatibility(this);
    }

    protected boolean checkCompatibility(LegacyEnchantment other) {
        return this != other;
    }

    /** Calls the mod's (protected) compatibility check for the pack generator. */
    boolean mcc$checkCompatibility(LegacyEnchantment other) {
        return checkCompatibility(other);
    }

    protected String getOrCreateDescriptionId() {
        if (descriptionId == null) descriptionId = Util.makeDescriptionId("enchantment", LegacyEnchantments.idOf(this));
        return descriptionId;
    }

    public String getDescriptionId() {
        return getOrCreateDescriptionId();
    }

    public Component getFullname(int level) {
        MutableComponent name = Component.translatable(getDescriptionId());
        name.withStyle(isCurse() ? ChatFormatting.RED : ChatFormatting.GRAY);
        if (level != 1 || getMaxLevel() != 1) name.append(CommonComponents.SPACE).append(Component.translatable("enchantment.level." + level));
        return name;
    }

    public boolean canEnchant(ItemStack stack) {
        return category != null && category.canEnchant(stack.getItem());
    }

    public void doPostAttack(LivingEntity attacker, Entity target, int level) {
    }

    public void doPostHurt(LivingEntity victim, Entity attacker, int level) {
    }

    public boolean isTreasureOnly() {
        return false;
    }

    public boolean isCurse() {
        return false;
    }

    public boolean isTradeable() {
        return true;
    }

    public boolean isDiscoverable() {
        return true;
    }

    public boolean isAllowedOnBooks() {
        return true;
    }

    // ------------------------------------------------------------------ bridge to 1.21.1

    /** The holder of this enchantment in the running game's data-driven registry. */
    public Holder<Enchantment> holder() {
        if (vanilla != null) return vanilla;
        ResourceLocation id = LegacyEnchantments.idOf(this);
        if (id == null) throw new IllegalStateException("enchantment " + getClass().getName() + " is not registered");
        return MigrateContext.registries().registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(ResourceKey.create(Registries.ENCHANTMENT, id));
    }
}
