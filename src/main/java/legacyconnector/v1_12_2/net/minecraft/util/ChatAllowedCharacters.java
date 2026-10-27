package legacyconnector.v1_12_2.net.minecraft.util;

public class ChatAllowedCharacters {
    public static final io.netty.util.ResourceLeakDetector.Level NETTY_LEAK_DETECTION = io.netty.util.ResourceLeakDetector.Level.DISABLED;
    public static final char[] ILLEGAL_STRUCTURE_CHARACTERS = new char[] {'.', '\n', '\r', '\t', '\u0000', '\f', '`', '?', '*', '\\', '<', '>', '|', '"'};
    public static final char[] ILLEGAL_FILE_CHARACTERS = new char[] {'/', '\n', '\r', '\t', '\u0000', '\f', '`', '?', '*', '\\', '<', '>', '|', '"', ':'};

    public static boolean isAllowedCharacter(char character) {
        return character != 167 && character >= ' ' && character != 127;
    }

    public static String filterAllowedCharacters(String input) {
        StringBuilder sb = new StringBuilder();
        for (char c : input.toCharArray()) if (isAllowedCharacter(c)) sb.append(c);
        return sb.toString();
    }
}
