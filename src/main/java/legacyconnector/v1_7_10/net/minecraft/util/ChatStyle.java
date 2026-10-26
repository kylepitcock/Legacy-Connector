package legacyconnector.v1_7_10.net.minecraft.util;

import net.minecraft.network.chat.Style;

/** Style subset: color and decorations. Click events (url, command) and text hover events are translated. */
public class ChatStyle {
    private EnumChatFormatting color;
    private Boolean bold;
    private Boolean italic;
    private Boolean underlined;
    private Boolean strikethrough;
    private Boolean obfuscated;
    private legacyconnector.v1_7_10.net.minecraft.event.ClickEvent clickEvent;
    private legacyconnector.v1_7_10.net.minecraft.event.HoverEvent hoverEvent;

    public legacyconnector.v1_7_10.net.minecraft.event.ClickEvent getChatClickEvent() {
        return clickEvent;
    }

    public ChatStyle setChatClickEvent(legacyconnector.v1_7_10.net.minecraft.event.ClickEvent event) {
        clickEvent = event;
        return this;
    }

    public legacyconnector.v1_7_10.net.minecraft.event.HoverEvent getChatHoverEvent() {
        return hoverEvent;
    }

    public ChatStyle setChatHoverEvent(legacyconnector.v1_7_10.net.minecraft.event.HoverEvent event) {
        hoverEvent = event;
        return this;
    }

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
        s.clickEvent = clickEvent;
        s.hoverEvent = hoverEvent;
        return s;
    }

    public ChatStyle createDeepCopy() {
        return createShallowCopy();
    }

    /** Whether nothing is set (the style inherits everything). */
    public boolean isEmpty() {
        return bold == null && italic == null && strikethrough == null && underlined == null && obfuscated == null && color == null
                && clickEvent == null && hoverEvent == null;
    }

    /** The Gson adapter for styles in the 1.7.10 JSON layout (colours by lower-case name, click and hover events as objects). */
    public static class Serializer implements com.google.gson.JsonDeserializer<ChatStyle>, com.google.gson.JsonSerializer<ChatStyle> {
        @Override
        public ChatStyle deserialize(com.google.gson.JsonElement json, java.lang.reflect.Type type, com.google.gson.JsonDeserializationContext context) {
            if (!json.isJsonObject()) return null;
            com.google.gson.JsonObject o = json.getAsJsonObject();
            ChatStyle s = new ChatStyle();
            if (o.has("bold")) s.bold = o.get("bold").getAsBoolean();
            if (o.has("italic")) s.italic = o.get("italic").getAsBoolean();
            if (o.has("underlined")) s.underlined = o.get("underlined").getAsBoolean();
            if (o.has("strikethrough")) s.strikethrough = o.get("strikethrough").getAsBoolean();
            if (o.has("obfuscated")) s.obfuscated = o.get("obfuscated").getAsBoolean();
            if (o.has("color")) {
                com.google.gson.JsonElement c = o.get("color");
                s.color = c.isJsonPrimitive() ? EnumChatFormatting.getValueByName(c.getAsString()) : null;
            }
            if (o.has("clickEvent") && o.get("clickEvent").isJsonObject()) {
                com.google.gson.JsonObject c = o.getAsJsonObject("clickEvent");
                var action = c.has("action") ? legacyconnector.v1_7_10.net.minecraft.event.ClickEvent.Action.getValueByCanonicalName(c.get("action").getAsString()) : null;
                String value = c.has("value") ? c.get("value").getAsString() : null;
                if (action != null && value != null && action.shouldAllowInChat()) s.clickEvent = new legacyconnector.v1_7_10.net.minecraft.event.ClickEvent(action, value);
            }
            if (o.has("hoverEvent") && o.get("hoverEvent").isJsonObject()) {
                com.google.gson.JsonObject h = o.getAsJsonObject("hoverEvent");
                var action = h.has("action") ? legacyconnector.v1_7_10.net.minecraft.event.HoverEvent.Action.getValueByCanonicalName(h.get("action").getAsString()) : null;
                IChatComponent value = h.has("value") ? context.deserialize(h.get("value"), IChatComponent.class) : null;
                if (action != null && value != null && action.shouldAllowInChat()) s.hoverEvent = new legacyconnector.v1_7_10.net.minecraft.event.HoverEvent(action, value);
            }
            return s;
        }

        @Override
        public com.google.gson.JsonElement serialize(ChatStyle s, java.lang.reflect.Type type, com.google.gson.JsonSerializationContext context) {
            if (s.isEmpty()) return null;
            com.google.gson.JsonObject o = new com.google.gson.JsonObject();
            if (s.bold != null) o.addProperty("bold", s.bold);
            if (s.italic != null) o.addProperty("italic", s.italic);
            if (s.underlined != null) o.addProperty("underlined", s.underlined);
            if (s.strikethrough != null) o.addProperty("strikethrough", s.strikethrough);
            if (s.obfuscated != null) o.addProperty("obfuscated", s.obfuscated);
            if (s.color != null) o.addProperty("color", s.color.getFriendlyName());
            if (s.clickEvent != null) {
                com.google.gson.JsonObject c = new com.google.gson.JsonObject();
                c.addProperty("action", s.clickEvent.getAction().getCanonicalName());
                c.addProperty("value", s.clickEvent.getValue());
                o.add("clickEvent", c);
            }
            if (s.hoverEvent != null) {
                com.google.gson.JsonObject h = new com.google.gson.JsonObject();
                h.addProperty("action", s.hoverEvent.getAction().getCanonicalName());
                h.add("value", context.serialize(s.hoverEvent.getValue(), IChatComponent.class));
                o.add("hoverEvent", h);
            }
            return o;
        }
    }

    public Style mcc$modern() {
        Style s = Style.EMPTY;
        if (color != null && color.mcc$modern() != null) s = s.withColor(color.mcc$modern());
        if (bold != null) s = s.withBold(bold);
        if (italic != null) s = s.withItalic(italic);
        if (underlined != null) s = s.withUnderlined(underlined);
        if (strikethrough != null) s = s.withStrikethrough(strikethrough);
        if (obfuscated != null) s = s.withObfuscated(obfuscated);
        if (clickEvent != null && clickEvent.mcc$modern() != null) s = s.withClickEvent(clickEvent.mcc$modern());
        if (hoverEvent != null && hoverEvent.mcc$modern() != null) s = s.withHoverEvent(hoverEvent.mcc$modern());
        return s;
    }
}
