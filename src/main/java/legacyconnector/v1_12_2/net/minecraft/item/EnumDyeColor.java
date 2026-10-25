package legacyconnector.v1_12_2.net.minecraft.item;

import legacyconnector.v1_12_2.net.minecraft.util.IStringSerializable;

public enum EnumDyeColor implements IStringSerializable {
    WHITE(0, 15, "white", "white", 0xF9FFFE),
    ORANGE(1, 14, "orange", "orange", 0xF9801D),
    MAGENTA(2, 13, "magenta", "magenta", 0xC74EBD),
    LIGHT_BLUE(3, 12, "light_blue", "lightBlue", 0x3AB3DA),
    YELLOW(4, 11, "yellow", "yellow", 0xFED83D),
    LIME(5, 10, "lime", "lime", 0x80C71F),
    PINK(6, 9, "pink", "pink", 0xF38BAA),
    GRAY(7, 8, "gray", "gray", 0x474F52),
    SILVER(8, 7, "silver", "silver", 0x9D9D97),
    CYAN(9, 6, "cyan", "cyan", 0x169C9C),
    PURPLE(10, 5, "purple", "purple", 0x8932B8),
    BLUE(11, 4, "blue", "blue", 0x3C44AA),
    BROWN(12, 3, "brown", "brown", 0x835432),
    GREEN(13, 2, "green", "green", 0x5E7C16),
    RED(14, 1, "red", "red", 0xB02E26),
    BLACK(15, 0, "black", "black", 0x1D1D21);

    private static final EnumDyeColor[] META_LOOKUP = new EnumDyeColor[16];
    private static final EnumDyeColor[] DYE_DMG_LOOKUP = new EnumDyeColor[16];

    static {
        for (EnumDyeColor c : values()) {
            META_LOOKUP[c.getMetadata()] = c;
            DYE_DMG_LOOKUP[c.getDyeDamage()] = c;
        }
    }

    private final int meta;
    private final int dyeDamage;
    private final String name;
    private final String unlocalizedName;
    private final int color;
    /** The 1.12 field mods read by reflection (field_193351_w). */
    private final int colorValue;

    EnumDyeColor(int meta, int dyeDamage, String name, String unlocalizedName, int color) {
        this.meta = meta;
        this.dyeDamage = dyeDamage;
        this.name = name;
        this.unlocalizedName = unlocalizedName;
        this.color = color;
        this.colorValue = color;
    }

    public int getMetadata() {
        return meta;
    }

    public int getDyeDamage() {
        return dyeDamage;
    }

    public String getTranslationKey() {
        return unlocalizedName;
    }

    public String getUnlocalizedName() {
        return unlocalizedName;
    }

    public int getColorValue() {
        return color;
    }

    public float[] getColorComponentValues() {
        return new float[] {(color >> 16 & 255) / 255.0F, (color >> 8 & 255) / 255.0F, (color & 255) / 255.0F};
    }

    public static EnumDyeColor byMetadata(int meta) {
        return META_LOOKUP[Math.abs(meta % META_LOOKUP.length)];
    }

    public static EnumDyeColor byDyeDamage(int damage) {
        return DYE_DMG_LOOKUP[Math.abs(damage % DYE_DMG_LOOKUP.length)];
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return unlocalizedName;
    }
}
