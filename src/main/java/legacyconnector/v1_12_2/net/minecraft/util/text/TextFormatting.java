package legacyconnector.v1_12_2.net.minecraft.util.text;

import java.util.Locale;

/** Chat colour and style codes. */
public enum TextFormatting {
    BLACK("BLACK", '0', 0),
    DARK_BLUE("DARK_BLUE", '1', 1),
    DARK_GREEN("DARK_GREEN", '2', 2),
    DARK_AQUA("DARK_AQUA", '3', 3),
    DARK_RED("DARK_RED", '4', 4),
    DARK_PURPLE("DARK_PURPLE", '5', 5),
    GOLD("GOLD", '6', 6),
    GRAY("GRAY", '7', 7),
    DARK_GRAY("DARK_GRAY", '8', 8),
    BLUE("BLUE", '9', 9),
    GREEN("GREEN", 'a', 10),
    AQUA("AQUA", 'b', 11),
    RED("RED", 'c', 12),
    LIGHT_PURPLE("LIGHT_PURPLE", 'd', 13),
    YELLOW("YELLOW", 'e', 14),
    WHITE("WHITE", 'f', 15),
    OBFUSCATED("OBFUSCATED", 'k', true),
    BOLD("BOLD", 'l', true),
    STRIKETHROUGH("STRIKETHROUGH", 'm', true),
    UNDERLINE("UNDERLINE", 'n', true),
    ITALIC("ITALIC", 'o', true),
    RESET("RESET", 'r', -1);

    private final String friendlyName;
    private final char formattingCode;
    private final boolean fancyStyling;
    private final String controlString;
    private final int colorIndex;

    TextFormatting(String friendlyName, char code, int colorIndex) {
        this(friendlyName, code, false, colorIndex);
    }

    TextFormatting(String friendlyName, char code, boolean fancy) {
        this(friendlyName, code, fancy, -1);
    }

    TextFormatting(String friendlyName, char code, boolean fancy, int colorIndex) {
        this.friendlyName = friendlyName;
        this.formattingCode = code;
        this.fancyStyling = fancy;
        this.colorIndex = colorIndex;
        this.controlString = "§" + code;
    }

    public int getColorIndex() {
        return colorIndex;
    }

    public boolean isFancyStyling() {
        return fancyStyling;
    }

    public boolean isColor() {
        return !fancyStyling && this != RESET;
    }

    public String getFriendlyName() {
        return name().toLowerCase(Locale.ROOT);
    }

    @Override
    public String toString() {
        return controlString;
    }

    public static String getTextWithoutFormattingCodes(String text) {
        return text == null ? null : text.replaceAll("(?i)§[0-9A-FK-OR]", "");
    }

    public static TextFormatting getValueByName(String friendlyName) {
        if (friendlyName == null) return null;
        for (TextFormatting f : values()) if (f.friendlyName.equalsIgnoreCase(friendlyName)) return f;
        return null;
    }

    public static TextFormatting fromColorIndex(int index) {
        if (index < 0) return RESET;
        for (TextFormatting f : values()) if (f.colorIndex == index) return f;
        return null;
    }
}
