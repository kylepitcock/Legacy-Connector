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
    private legacyconnector.v1_12_2.net.minecraft.util.text.event.ClickEvent clickEvent;
    private legacyconnector.v1_12_2.net.minecraft.util.text.event.HoverEvent hoverEvent;

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
        public legacyconnector.v1_12_2.net.minecraft.util.text.event.ClickEvent getClickEvent() {
            return null;
        }

        @Override
        public legacyconnector.v1_12_2.net.minecraft.util.text.event.HoverEvent getHoverEvent() {
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
                && insertion == null && clickEvent == null && hoverEvent == null;
    }

    public legacyconnector.v1_12_2.net.minecraft.util.text.event.ClickEvent getClickEvent() {
        return clickEvent == null ? parent().getClickEvent() : clickEvent;
    }

    public legacyconnector.v1_12_2.net.minecraft.util.text.event.HoverEvent getHoverEvent() {
        return hoverEvent == null ? parent().getHoverEvent() : hoverEvent;
    }

    public Style setClickEvent(legacyconnector.v1_12_2.net.minecraft.util.text.event.ClickEvent event) {
        this.clickEvent = event;
        return this;
    }

    public Style setHoverEvent(legacyconnector.v1_12_2.net.minecraft.util.text.event.HoverEvent event) {
        this.hoverEvent = event;
        return this;
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
        s.clickEvent = clickEvent;
        s.hoverEvent = hoverEvent;
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
        s.setClickEvent(getClickEvent());
        s.setHoverEvent(getHoverEvent());
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
        if (getClickEvent() != null) s = s.withClickEvent(getClickEvent().mcc$modern());
        if (getHoverEvent() != null) s = s.withHoverEvent(getHoverEvent().mcc$modern());
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

    /** Writes the values set on this style (not inherited ones) into a 1.12 text JSON object. */
    public void mcc$writeJson(com.google.gson.JsonObject o) {
        if (bold != null) o.addProperty("bold", bold);
        if (italic != null) o.addProperty("italic", italic);
        if (underlined != null) o.addProperty("underlined", underlined);
        if (strikethrough != null) o.addProperty("strikethrough", strikethrough);
        if (obfuscated != null) o.addProperty("obfuscated", obfuscated);
        if (color != null) o.addProperty("color", color.getFriendlyName());
        if (insertion != null) o.addProperty("insertion", insertion);
        if (clickEvent != null) {
            com.google.gson.JsonObject e = new com.google.gson.JsonObject();
            e.addProperty("action", clickEvent.getAction().getCanonicalName());
            e.addProperty("value", clickEvent.getValue());
            o.add("clickEvent", e);
        }
        if (hoverEvent != null) {
            com.google.gson.JsonObject e = new com.google.gson.JsonObject();
            e.addProperty("action", hoverEvent.getAction().getCanonicalName());
            e.add("value", ITextComponent.Serializer.mcc$toTree(hoverEvent.getValue()));
            o.add("hoverEvent", e);
        }
    }

    /** The style a 1.12 text JSON object describes. */
    public static Style mcc$readJson(com.google.gson.JsonObject o) {
        Style s = new Style();
        if (o.has("bold")) s.bold = o.get("bold").getAsBoolean();
        if (o.has("italic")) s.italic = o.get("italic").getAsBoolean();
        if (o.has("underlined")) s.underlined = o.get("underlined").getAsBoolean();
        if (o.has("strikethrough")) s.strikethrough = o.get("strikethrough").getAsBoolean();
        if (o.has("obfuscated")) s.obfuscated = o.get("obfuscated").getAsBoolean();
        if (o.has("color")) s.color = TextFormatting.getValueByName(o.get("color").getAsString());
        if (o.has("insertion")) s.insertion = o.get("insertion").getAsString();
        if (o.has("clickEvent") && o.get("clickEvent").isJsonObject()) {
            com.google.gson.JsonObject e = o.getAsJsonObject("clickEvent");
            var action = e.has("action") ? legacyconnector.v1_12_2.net.minecraft.util.text.event.ClickEvent.Action
                    .getValueByCanonicalName(e.get("action").getAsString()) : null;
            String value = e.has("value") ? e.get("value").getAsString() : null;
            if (action != null && value != null && action.shouldAllowInChat()) {
                s.clickEvent = new legacyconnector.v1_12_2.net.minecraft.util.text.event.ClickEvent(action, value);
            }
        }
        if (o.has("hoverEvent") && o.get("hoverEvent").isJsonObject()) {
            com.google.gson.JsonObject e = o.getAsJsonObject("hoverEvent");
            var action = e.has("action") ? legacyconnector.v1_12_2.net.minecraft.util.text.event.HoverEvent.Action
                    .getValueByCanonicalName(e.get("action").getAsString()) : null;
            ITextComponent value = e.has("value") ? ITextComponent.Serializer.mcc$fromTree(e.get("value")) : null;
            if (action != null && value != null && action.shouldAllowInChat()) {
                s.hoverEvent = new legacyconnector.v1_12_2.net.minecraft.util.text.event.HoverEvent(action, value);
            }
        }
        return s;
    }
}
