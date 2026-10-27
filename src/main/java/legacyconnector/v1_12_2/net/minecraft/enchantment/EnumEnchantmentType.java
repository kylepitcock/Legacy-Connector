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
        if (delegate != null) return delegate.apply(item);
        return mcc$vanilla(item);
    }

    /** The vanilla types' rules (1.12's anonymous enum bodies). */
    private boolean mcc$vanilla(Item item) {
        if (item == null) return false;
        var path = item.getRegistryName() == null ? "" : item.getRegistryName().getPath();
        var armor = item instanceof legacyconnector.v1_12_2.net.minecraft.item.ItemArmor a ? a.armorType : null;
        return switch (name) {
            case "ALL" -> {
                for (EnumEnchantmentType t : values()) if (t != this && t.canEnchantItem(item)) yield true;
                yield false;
            }
            case "ARMOR" -> armor != null;
            case "ARMOR_FEET" -> armor == legacyconnector.v1_12_2.net.minecraft.inventory.EntityEquipmentSlot.FEET;
            case "ARMOR_LEGS" -> armor == legacyconnector.v1_12_2.net.minecraft.inventory.EntityEquipmentSlot.LEGS;
            case "ARMOR_CHEST" -> armor == legacyconnector.v1_12_2.net.minecraft.inventory.EntityEquipmentSlot.CHEST;
            case "ARMOR_HEAD" -> armor == legacyconnector.v1_12_2.net.minecraft.inventory.EntityEquipmentSlot.HEAD;
            case "WEAPON" -> item instanceof legacyconnector.v1_12_2.net.minecraft.item.ItemSword;
            case "DIGGER" -> item instanceof legacyconnector.v1_12_2.net.minecraft.item.ItemTool;
            case "FISHING_ROD" -> path.equals("fishing_rod") || mcc$isa(item, "ItemFishingRod");
            case "BREAKABLE" -> item.isDamageable();
            case "BOW" -> path.equals("bow") || mcc$isa(item, "ItemBow");
            case "WEARABLE" -> armor != null || path.equals("elytra") || path.equals("pumpkin") || path.equals("skull") || mcc$isa(item, "ItemElytra");
            default -> true;
        };
    }

    /** Whether the item's class extends the vanilla class of that simple name (synthesized vanilla classes such as ItemBow). */
    private static boolean mcc$isa(Item item, String simpleName) {
        for (Class<?> c = item.getClass(); c != null && c != Item.class; c = c.getSuperclass()) {
            if (c.getName().endsWith("net.minecraft.item." + simpleName)) return true;
        }
        return false;
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

    /** {@code Enum.compareTo}: legacy calls taking an Enum are linked here. */
    public int compareTo(Object other) {
        return Integer.compare(ordinal(), ((EnumEnchantmentType) other).ordinal());
    }

    public int ordinal() {
        return ordinal;
    }

    public static EnumEnchantmentType valueOf(String name) {
        for (EnumEnchantmentType v : values()) if (v.name().equals(name)) return v;
        throw new IllegalArgumentException("No enum constant " + name);
    }
}
