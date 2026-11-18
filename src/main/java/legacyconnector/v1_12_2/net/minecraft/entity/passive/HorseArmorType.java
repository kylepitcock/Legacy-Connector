package legacyconnector.v1_12_2.net.minecraft.entity.passive;

import legacyconnector.v1_12_2.net.minecraft.init.Items;
import legacyconnector.v1_12_2.net.minecraft.item.Item;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;

/** The horse armour tiers: texture, hash (the texture key of a horse's layers) and the armour points each gives. */
public enum HorseArmorType {
    NONE(0),
    IRON(5, "iron", "meo"),
    GOLD(7, "gold", "goo"),
    DIAMOND(11, "diamond", "dio");

    private final String textureName;
    private final String hash;
    private final int protection;

    HorseArmorType(int protection) {
        this.protection = protection;
        this.textureName = null;
        this.hash = "";
    }

    /** An armour added by {@code EnumHelper.addHorseArmor}: the texture is a full resource path. */
    HorseArmorType(int protection, String name, String hash, String texturePath) {
        this.protection = protection;
        this.textureName = texturePath;
        this.hash = hash;
    }

    HorseArmorType(int protection, String name, String hash) {
        this.protection = protection;
        this.textureName = "textures/entity/horse/armor/horse_armor_" + name + ".png";
        this.hash = hash;
    }

    public int getOrdinal() {
        return ordinal();
    }

    public String getHash() {
        return hash;
    }

    public int getProtection() {
        return protection;
    }

    public String getTextureName() {
        return textureName;
    }

    public static HorseArmorType getByOrdinal(int ordinal) {
        return values()[ordinal];
    }

    public static HorseArmorType getByItemStack(ItemStack stack) {
        return stack == null || stack.isEmpty() ? NONE : getByItem(stack.getItem());
    }

    public static HorseArmorType getByItem(Item item) {
        if (item == Items.IRON_HORSE_ARMOR) return IRON;
        if (item == Items.GOLDEN_HORSE_ARMOR) return GOLD;
        if (item == Items.DIAMOND_HORSE_ARMOR) return DIAMOND;
        return NONE;
    }

    public static boolean isHorseArmor(ItemStack stack) {
        return getByItemStack(stack) != NONE;
    }

    public static boolean isHorseArmor(Item item) {
        return getByItem(item) != NONE;
    }

}
