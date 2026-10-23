package legacyconnector.v1_7_10.net.minecraft.util;

/** Which characters a text field accepts. */
public class ChatAllowedCharacters {
    public static final char[] allowedCharacters = {'/', '\n', '\r', '\t', '\u0000', '\f', '`', '?', '*', '\\', '<', '>', '|', '"', ':'};

    public static boolean isAllowedCharacter(char c) {
        return c != 167 && c >= ' ' && c != 127;
    }

    public static String filerAllowedCharacters(String text) {
        StringBuilder out = new StringBuilder();
        for (char c : text.toCharArray()) if (isAllowedCharacter(c)) out.append(c);
        return out.toString();
    }
}
