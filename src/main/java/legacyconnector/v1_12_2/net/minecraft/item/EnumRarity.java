package legacyconnector.v1_12_2.net.minecraft.item;

import legacyconnector.v1_12_2.net.minecraft.util.text.TextFormatting;

/** Item rarity: the colour of the name (an enum in 1.12.2; a class here so mods can add their own through EnumHelper). */
public class EnumRarity implements legacyconnector.v1_12_2.net.minecraftforge.common.IRarity {
    private static final java.util.List<EnumRarity> ALL = new java.util.ArrayList<>();
    private final int ordinal;

    public static final EnumRarity COMMON = new EnumRarity("COMMON", TextFormatting.WHITE, "Common");
    public static final EnumRarity UNCOMMON = new EnumRarity("UNCOMMON", TextFormatting.YELLOW, "Uncommon");
    public static final EnumRarity RARE = new EnumRarity("RARE", TextFormatting.AQUA, "Rare");
    public static final EnumRarity EPIC = new EnumRarity("EPIC", TextFormatting.LIGHT_PURPLE, "Epic");

    public final TextFormatting color;
    public final String rarityName;
    private final String name;

    public EnumRarity(String name, TextFormatting color, String displayName) {
        this.ordinal = ALL.size();
        ALL.add(this);
        this.name = name;
        this.color = color;
        this.rarityName = displayName;
    }

    public String name() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }

    public static EnumRarity[] values() {
        synchronized (ALL) {
            return ALL.toArray(new EnumRarity[0]);
        }
    }

    /** {@code Enum.compareTo}: legacy calls taking an Enum are linked here. */
    public int compareTo(Object other) {
        return Integer.compare(ordinal(), ((EnumRarity) other).ordinal());
    }

    public int ordinal() {
        return ordinal;
    }

    public static EnumRarity valueOf(String name) {
        for (EnumRarity v : values()) if (v.name().equals(name)) return v;
        throw new IllegalArgumentException("No enum constant " + name);
    }

    /** Forge's IRarity (area F). */
    @Override
    public TextFormatting getColor() {
        return color;
    }

    @Override
    public String getName() {
        return rarityName;
    }
}
