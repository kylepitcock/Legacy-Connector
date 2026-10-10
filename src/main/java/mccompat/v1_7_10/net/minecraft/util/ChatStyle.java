package mccompat.v1_7_10.net.minecraft.util;

import net.minecraft.network.chat.Style;

/** Style subset: color and decorations. Click/hover events are not translated yet. */
public class ChatStyle {
    private EnumChatFormatting color;
    private Boolean bold;
    private Boolean italic;
    private Boolean underlined;
    private Boolean strikethrough;
    private Boolean obfuscated;

    public EnumChatFormatting getColor() {
        return color;
    }

    public boolean getBold() {
        return Boolean.TRUE.equals(bold);
    }

    public boolean getItalic() {
        return Boolean.TRUE.equals(italic);
    }

    public boolean getUnderlined() {
        return Boolean.TRUE.equals(underlined);
    }

    public boolean getStrikethrough() {
        return Boolean.TRUE.equals(strikethrough);
    }

    public boolean getObfuscated() {
        return Boolean.TRUE.equals(obfuscated);
    }

    public ChatStyle setColor(EnumChatFormatting color) {
        this.color = color;
        return this;
    }

    public ChatStyle setBold(Boolean v) {
        bold = v;
        return this;
    }

    public ChatStyle setItalic(Boolean v) {
        italic = v;
        return this;
    }

    public ChatStyle setUnderlined(Boolean v) {
        underlined = v;
        return this;
    }

    public ChatStyle setStrikethrough(Boolean v) {
        strikethrough = v;
        return this;
    }

    public ChatStyle setObfuscated(Boolean v) {
        obfuscated = v;
        return this;
    }

    public ChatStyle createShallowCopy() {
        ChatStyle s = new ChatStyle();
        s.color = color;
        s.bold = bold;
        s.italic = italic;
        s.underlined = underlined;
        s.strikethrough = strikethrough;
        s.obfuscated = obfuscated;
        return s;
    }

    public ChatStyle createDeepCopy() {
        return createShallowCopy();
    }

    public Style mcc$modern() {
        Style s = Style.EMPTY;
        if (color != null && color.mcc$modern() != null) s = s.withColor(color.mcc$modern());
        if (bold != null) s = s.withBold(bold);
        if (italic != null) s = s.withItalic(italic);
        if (underlined != null) s = s.withUnderlined(underlined);
        if (strikethrough != null) s = s.withStrikethrough(strikethrough);
        if (obfuscated != null) s = s.withObfuscated(obfuscated);
        return s;
    }
}
