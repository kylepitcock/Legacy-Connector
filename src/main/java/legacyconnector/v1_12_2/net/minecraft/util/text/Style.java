package legacyconnector.v1_12_2.net.minecraft.util.text;

import java.util.Objects;

/** The formatting of a text component; unset values are inherited from the parent style (the root style has none). */
public class Style {
    private Style parentStyle;
    private TextFormatting color;
    private Boolean bold;
    private Boolean italic;
    private Boolean underlined;
    private Boolean strikethrough;
    private Boolean obfuscated;
    private String insertion;

    private static final Style ROOT = new Style() {
        @Override
        public TextFormatting getColor() {
            return null;
        }

        @Override
        public boolean getBold() {
            return false;
        }

        @Override
        public boolean getItalic() {
            return false;
        }

        @Override
        public boolean getStrikethrough() {
            return false;
        }

        @Override
        public boolean getUnderlined() {
            return false;
        }

        @Override
        public boolean getObfuscated() {
            return false;
        }

        @Override
        public String getInsertion() {
            return null;
        }

        @Override
        public Style setParentStyle(Style parent) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getFormattingCode() {
            return "";
        }

        @Override
        public String toString() {
            return "Style.ROOT";
        }
    };

    private Style parent() {
        return parentStyle == null ? ROOT : parentStyle;
    }

    public TextFormatting getColor() {
        return color == null ? parent().getColor() : color;
    }

    public boolean getBold() {
        return bold == null ? parent().getBold() : bold;
    }

    public boolean getItalic() {
        return italic == null ? parent().getItalic() : italic;
    }

    public boolean getStrikethrough() {
        return strikethrough == null ? parent().getStrikethrough() : strikethrough;
    }

    public boolean getUnderlined() {
        return underlined == null ? parent().getUnderlined() : underlined;
    }

    public boolean getObfuscated() {
        return obfuscated == null ? parent().getObfuscated() : obfuscated;
    }

    public boolean isEmpty() {
        return bold == null && italic == null && strikethrough == null && underlined == null && obfuscated == null && color == null
                && insertion == null;
    }

    public String getInsertion() {
        return insertion == null ? parent().getInsertion() : insertion;
    }

    public Style setColor(TextFormatting color) {
        this.color = color;
        return this;
    }

    public Style setBold(Boolean bold) {
        this.bold = bold;
        return this;
    }

    public Style setItalic(Boolean italic) {
        this.italic = italic;
        return this;
    }

    public Style setStrikethrough(Boolean strikethrough) {
        this.strikethrough = strikethrough;
        return this;
    }

    public Style setUnderlined(Boolean underlined) {
        this.underlined = underlined;
        return this;
    }

    public Style setObfuscated(Boolean obfuscated) {
        this.obfuscated = obfuscated;
        return this;
    }

    public Style setInsertion(String insertion) {
        this.insertion = insertion;
        return this;
    }

    public Style setParentStyle(Style parent) {
        this.parentStyle = parent;
        return this;
    }

    /** The section-sign codes of this style (colour first, then bold, italic, underline, obfuscated, strikethrough). */
    public String getFormattingCode() {
        if (isEmpty()) return parentStyle != null ? parentStyle.getFormattingCode() : "";
        StringBuilder sb = new StringBuilder();
        if (getColor() != null) sb.append(getColor());
        if (getBold()) sb.append(TextFormatting.BOLD);
        if (getItalic()) sb.append(TextFormatting.ITALIC);
        if (getUnderlined()) sb.append(TextFormatting.UNDERLINE);
        if (getObfuscated()) sb.append(TextFormatting.OBFUSCATED);
        if (getStrikethrough()) sb.append(TextFormatting.STRIKETHROUGH);
        return sb.toString();
    }

    public Style createShallowCopy() {
        Style s = new Style();
        s.bold = bold;
        s.italic = italic;
        s.strikethrough = strikethrough;
        s.underlined = underlined;
        s.obfuscated = obfuscated;
        s.color = color;
        s.insertion = insertion;
        s.parentStyle = parentStyle;
        return s;
    }

    /** A copy with every inherited value resolved. */
    public Style createDeepCopy() {
        Style s = new Style();
        s.setBold(getBold());
        s.setItalic(getItalic());
        s.setStrikethrough(getStrikethrough());
        s.setUnderlined(getUnderlined());
        s.setObfuscated(getObfuscated());
        s.setColor(getColor());
        s.setInsertion(getInsertion());
        return s;
    }

    /** The modern style (resolved values). */
    public net.minecraft.network.chat.Style mcc$modern() {
        var s = net.minecraft.network.chat.Style.EMPTY;
        TextFormatting c = getColor();
        if (c != null) {
            var f = net.minecraft.ChatFormatting.getByName(c.name());
            if (f != null) s = s.applyFormat(f);
        }
        if (getBold()) s = s.withBold(true);
        if (getItalic()) s = s.withItalic(true);
        if (getUnderlined()) s = s.withUnderlined(true);
        if (getStrikethrough()) s = s.withStrikethrough(true);
        if (getObfuscated()) s = s.withObfuscated(true);
        if (getInsertion() != null) s = s.withInsertion(getInsertion());
        return s;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Style s)) return false;
        return getBold() == s.getBold() && getColor() == s.getColor() && getItalic() == s.getItalic() && getObfuscated() == s.getObfuscated()
                && getStrikethrough() == s.getStrikethrough() && getUnderlined() == s.getUnderlined() && Objects.equals(getInsertion(), s.getInsertion());
    }

    @Override
    public int hashCode() {
        return Objects.hash(color, bold, italic, underlined, strikethrough, obfuscated, insertion);
    }

    @Override
    public String toString() {
        return "Style{hasParent=" + (parentStyle != null) + ", color=" + color + ", bold=" + bold + ", italic=" + italic + ", underlined=" + underlined
                + ", obfuscated=" + obfuscated + ", insertion=" + insertion + "}";
    }
}
