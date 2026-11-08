package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import java.util.function.Predicate;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TridentItem;

/**
 * Stands in for 1.20.1's {@code EnchantmentCategory} (gone in 1.21.1, where an enchantment lists its supported items). Forge added {@code create(name, predicate)}
 * to the enum; mods used it for their own categories.
 */
public class LegacyEnchantmentCategory {
    public static final LegacyEnchantmentCategory ARMOR = new LegacyEnchantmentCategory("ARMOR", i -> i instanceof ArmorItem);
    public static final LegacyEnchantmentCategory ARMOR_FEET = new LegacyEnchantmentCategory("ARMOR_FEET", i -> i instanceof ArmorItem a && a.getType() == ArmorItem.Type.BOOTS);
    public static final LegacyEnchantmentCategory ARMOR_LEGS = new LegacyEnchantmentCategory("ARMOR_LEGS", i -> i instanceof ArmorItem a && a.getType() == ArmorItem.Type.LEGGINGS);
    public static final LegacyEnchantmentCategory ARMOR_CHEST = new LegacyEnchantmentCategory("ARMOR_CHEST", i -> i instanceof ArmorItem a && a.getType() == ArmorItem.Type.CHESTPLATE);
    public static final LegacyEnchantmentCategory ARMOR_HEAD = new LegacyEnchantmentCategory("ARMOR_HEAD", i -> i instanceof ArmorItem a && a.getType() == ArmorItem.Type.HELMET);
    public static final LegacyEnchantmentCategory WEAPON = new LegacyEnchantmentCategory("WEAPON", i -> i instanceof SwordItem);
    public static final LegacyEnchantmentCategory DIGGER = new LegacyEnchantmentCategory("DIGGER", i -> i instanceof DiggerItem);
    public static final LegacyEnchantmentCategory FISHING_ROD = new LegacyEnchantmentCategory("FISHING_ROD", i -> i instanceof FishingRodItem);
    public static final LegacyEnchantmentCategory TRIDENT = new LegacyEnchantmentCategory("TRIDENT", i -> i instanceof TridentItem);
    public static final LegacyEnchantmentCategory BREAKABLE = new LegacyEnchantmentCategory("BREAKABLE", LegacyEnchantmentCategory::damageable);
    public static final LegacyEnchantmentCategory BOW = new LegacyEnchantmentCategory("BOW", i -> i instanceof BowItem);
    public static final LegacyEnchantmentCategory WEARABLE = new LegacyEnchantmentCategory("WEARABLE", LegacyEnchantmentCategory::wearable);
    public static final LegacyEnchantmentCategory CROSSBOW = new LegacyEnchantmentCategory("CROSSBOW", i -> i instanceof CrossbowItem);
    public static final LegacyEnchantmentCategory VANISHABLE = new LegacyEnchantmentCategory("VANISHABLE", i -> damageable(i) || wearable(i));

    private final String name;
    private final Predicate<Item> delegate;

    private LegacyEnchantmentCategory(String name, Predicate<Item> delegate) {
        this.name = name;
        this.delegate = delegate;
    }

    private static boolean damageable(Item i) {
        return i.components().has(net.minecraft.core.component.DataComponents.MAX_DAMAGE);
    }

    private static boolean wearable(Item i) {
        return i instanceof Equipable || (i instanceof BlockItem b && b.getBlock() instanceof Equipable);
    }

    /** Forge's enum extension. */
    public static LegacyEnchantmentCategory create(String name, Predicate<Item> delegate) {
        return new LegacyEnchantmentCategory(name, delegate);
    }

    public boolean canEnchant(Item item) {
        return delegate.test(item);
    }

    public String name() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }
}
