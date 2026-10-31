package legacyconnector.v1_12_2.com.mojang.realmsclient.gui;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Realms' copy of the chat formatting codes ({@code com.mojang.realmsclient.gui.ChatFormatting}): 1.8 to 1.12 mods used it
 * instead of {@code TextFormatting} (Kindred Legacy's tameable item GUI), and a modern game has no such class.
 */
public enum ChatFormatting {
    BLACK('0'), DARK_BLUE('1'), DARK_GREEN('2'), DARK_AQUA('3'), DARK_RED('4'), DARK_PURPLE('5'), GOLD('6'), GRAY('7'), DARK_GRAY('8'),
    BLUE('9'), GREEN('a'), AQUA('b'), RED('c'), LIGHT_PURPLE('d'), YELLOW('e'), WHITE('f'),
    OBFUSCATED('k', true), BOLD('l', true), STRIKETHROUGH('m', true), UNDERLINE('n', true), ITALIC('o', true), RESET('r');

    private static final Pattern STRIP = Pattern.compile("(?i)§[0-9A-FK-OR]");
    private final char code;
    private final boolean fancy;
    private final String control;

    ChatFormatting(char code) {
        this(code, false);
    }

    ChatFormatting(char code, boolean fancy) {
        this.code = code;
        this.fancy = fancy;
        this.control = "§" + code;
    }

    public char getChar() {
        return code;
    }

    public boolean isFancy() {
        return fancy;
    }

    public boolean isColor() {
        return !fancy && this != RESET;
    }

    public String getName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static String stripFormatting(String text) {
        return text == null ? null : STRIP.matcher(text).replaceAll("");
    }

    public static ChatFormatting getByChar(char c) {
        for (ChatFormatting f : values()) if (f.code == Character.toLowerCase(c)) return f;
        return null;
    }

    @Override
    public String toString() {
        return control;
    }
}
