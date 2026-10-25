package legacyconnector.v1_12_2.net.minecraft.enchantment;

import com.google.common.base.Predicate;
import legacyconnector.v1_12_2.net.minecraft.item.Item;

/** The kinds of item an enchantment applies to (an enum in 1.12.2; a class here so mods can add their own through EnumHelper). */
public class EnumEnchantmentType {
    private static final java.util.List<EnumEnchantmentType> VALUES = new java.util.ArrayList<>();
    private final int ordinal;

    public static final EnumEnchantmentType ALL = new EnumEnchantmentType("ALL", null);
    public static final EnumEnchantmentType ARMOR = new EnumEnchantmentType("ARMOR", null);
    public static final EnumEnchantmentType ARMOR_FEET = new EnumEnchantmentType("ARMOR_FEET", null);
    public static final EnumEnchantmentType ARMOR_LEGS = new EnumEnchantmentType("ARMOR_LEGS", null);
    public static final EnumEnchantmentType ARMOR_CHEST = new EnumEnchantmentType("ARMOR_CHEST", null);
    public static final EnumEnchantmentType ARMOR_HEAD = new EnumEnchantmentType("ARMOR_HEAD", null);
    public static final EnumEnchantmentType WEAPON = new EnumEnchantmentType("WEAPON", null);
    public static final EnumEnchantmentType DIGGER = new EnumEnchantmentType("DIGGER", null);
    public static final EnumEnchantmentType FISHING_ROD = new EnumEnchantmentType("FISHING_ROD", null);
    public static final EnumEnchantmentType BREAKABLE = new EnumEnchantmentType("BREAKABLE", null);
    public static final EnumEnchantmentType BOW = new EnumEnchantmentType("BOW", null);
    public static final EnumEnchantmentType WEARABLE = new EnumEnchantmentType("WEARABLE", null);

    private final String name;
    private final Predicate<Item> delegate;

    public EnumEnchantmentType(String name, Predicate<Item> delegate) {
        this.ordinal = VALUES.size();
        VALUES.add(this);
        this.name = name;
        this.delegate = delegate;
    }

    public String name() {
        return name;
    }

    public boolean canEnchantItem(Item item) {
        return delegate == null || delegate.apply(item);
    }

    @Override
    public String toString() {
        return name;
    }

    public static EnumEnchantmentType[] values() {
        synchronized (VALUES) {
            return VALUES.toArray(new EnumEnchantmentType[0]);
        }
    }

    public int ordinal() {
        return ordinal;
    }

    public static EnumEnchantmentType valueOf(String name) {
        for (EnumEnchantmentType v : values()) if (v.name().equals(name)) return v;
        throw new IllegalArgumentException("No enum constant " + name);
    }
}
