package org.kylepitcock.legacyconnector.v1_7_10.bridge;

import net.minecraft.network.chat.Component;

/**
 * Legacy strings on their way to modern text (item names, tooltips, hovering text, the game font's view).
 *
 * <p>DragonAPI replaces {@code mc.fontRenderer} with its {@code DelegateFontRenderer}: every custom font a mod registers
 * through {@code addRenderer} (ChromatiCraft's GUI, lexicon, HUD and obfuscated rune fonts, ...) gets a two-character key,
 * U+FFFC followed by the char {@code 0x200 + n}, and a string containing a key is drawn with that font, the key removed
 * ({@code DelegateFontRenderer.getRenderer}/{@code stripFlags}). ChromatiCraft prefixes the names of crystal items of
 * colours the player has not discovered with the obfuscated font's key. Modern text is drawn by the modern font, which
 * knows none of these fonts and would draw the key as a box and a stray letter ("ȃ"); the key is dropped instead, as
 * DragonAPI's {@code stripFlags} does. Legacy code keeps the keyed strings, so its own {@code drawString} calls on
 * {@code mc.fontRenderer} still reach the mod's font.
 */
public final class LegacyText {
    /** DragonAPI's font key marker ({@code DelegateFontRenderer.keyChar}). */
    public static final char FONT_KEY = '￼';

    private LegacyText() {
    }

    /** The text without DragonAPI font keys; the same string (no copy) when it has none. */
    public static String strip(String text) {
        if (text == null) return null;
        int i = text.indexOf(FONT_KEY);
        if (i < 0) return text;
        StringBuilder out = new StringBuilder(text.length());
        int from = 0;
        while (i >= 0) {
            out.append(text, from, i);
            // the key is the marker and the char after it; a marker at the very end is dropped alone
            from = Math.min(i + 2, text.length());
            i = text.indexOf(FONT_KEY, from);
        }
        out.append(text, from, text.length());
        return out.toString();
    }

    /** A literal component of legacy text (font keys dropped; section-sign codes are kept and styled by the modern font). */
    public static Component literal(String text) {
        return Component.literal(strip(text));
    }
}
