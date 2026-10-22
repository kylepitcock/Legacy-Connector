package legacyconnector.v1_7_10.net.minecraft.util;

import net.minecraft.ChatFormatting;

public enum EnumChatFormatting {
    BLACK('0'), DARK_BLUE('1'), DARK_GREEN('2'), DARK_AQUA('3'), DARK_RED('4'), DARK_PURPLE('5'), GOLD('6'), GRAY('7'), DARK_GRAY('8'),
    BLUE('9'), GREEN('a'), AQUA('b'), RED('c'), LIGHT_PURPLE('d'), YELLOW('e'), WHITE('f'), OBFUSCATED('k', true), BOLD('l', true),
    STRIKETHROUGH('m', true), UNDERLINE('n', true), ITALIC('o', true), RESET('r');

    private final char formattingCode;
    private final boolean fancyStyling;
    private final String controlString;

    EnumChatFormatting(char code) {
        this(code, false);
    }

    EnumChatFormatting(char code, boolean fancy) {
        this.formattingCode = code;
        this.fancyStyling = fancy;
        this.controlString = "§" + code;
    }

    public char getFormattingCode() {
        return formattingCode;
    }

    public boolean isFancyStyling() {
        return fancyStyling;
    }

    public boolean isColor() {
        return !fancyStyling && this != RESET;
    }

    public String getFriendlyName() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }

    @Override
    public String toString() {
        return controlString;
    }

    public static String getTextWithoutFormattingCodes(String text) {
        return text == null ? null : ChatFormatting.stripFormatting(text);
    }

    public static EnumChatFormatting getValueByName(String name) {
        if (name == null) return null;
        try {
            return valueOf(name.toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public ChatFormatting mcc$modern() {
        return ChatFormatting.getByCode(formattingCode);
    }
}
