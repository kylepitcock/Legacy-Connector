package legacyconnector.v1_6_4.facade.net.minecraft.util;

/** Facade of the 1.7.10 shim's {@code ChatAllowedCharacters} for 1.6.4 mods: {@code allowedCharacters} was the string of the characters the font has. Never instantiated. */
public final class ChatAllowedCharacters {
    private ChatAllowedCharacters() {}

    private static volatile String allowed;

    /** ChatAllowedCharacters.allowedCharacters: printable ASCII and Latin-1 (what the font drew), less the section sign that starts a format code. */
    public static String mcc$get$allowedCharacters() {
        String s = allowed;
        if (s == null) {
            StringBuilder out = new StringBuilder();
            for (char c = 32; c < 256; c++) {
                if (c >= 127 && c < 161) continue;
                if (legacyconnector.v1_7_10.net.minecraft.util.ChatAllowedCharacters.isAllowedCharacter(c)) out.append(c);
            }
            allowed = s = out.toString();
        }
        return s;
    }
}
