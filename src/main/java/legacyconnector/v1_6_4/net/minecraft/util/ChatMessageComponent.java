package legacyconnector.v1_6_4.net.minecraft.util;

import java.util.ArrayList;
import java.util.List;
import legacyconnector.v1_7_10.net.minecraft.util.ChatComponentText;
import legacyconnector.v1_7_10.net.minecraft.util.ChatComponentTranslation;
import legacyconnector.v1_7_10.net.minecraft.util.ChatStyle;
import legacyconnector.v1_7_10.net.minecraft.util.EnumChatFormatting;
import legacyconnector.v1_7_10.net.minecraft.util.IChatComponent;

/**
 * 1.6.4's chat message builder ({@code ChatMessageComponent.createFromText("x").setColor(...)}), which 1.7.10 replaced with
 * {@code IChatComponent}. Keeps the same tree (text or translation key with arguments, sub components, style) and turns it into the
 * 1.7.10 shim's component on use ({@link #mcc$toLegacy}), from which the modern component is built.
 */
public class ChatMessageComponent {
    private EnumChatFormatting color;
    private Boolean bold;
    private Boolean italic;
    private Boolean underline;
    private Boolean obfuscated;
    private String text;
    private String translationKey;
    private Object[] translationArgs;
    private final List<ChatMessageComponent> subComponents = new ArrayList<>();

    public ChatMessageComponent() {}

    public static ChatMessageComponent createFromText(String text) {
        ChatMessageComponent c = new ChatMessageComponent();
        c.addText(text);
        return c;
    }

    public static ChatMessageComponent createFromTranslationKey(String key) {
        ChatMessageComponent c = new ChatMessageComponent();
        c.addKey(key);
        return c;
    }

    public static ChatMessageComponent createFromTranslationWithSubstitutions(String key, Object... args) {
        ChatMessageComponent c = new ChatMessageComponent();
        c.addFormatted(key, args);
        return c;
    }

    public static ChatMessageComponent createFromJson(String json) {
        IChatComponent parsed = IChatComponent.Serializer.jsonToComponent(json);
        return createFromText(parsed == null ? "" : parsed.getUnformattedText());
    }

    public ChatMessageComponent setColor(EnumChatFormatting color) {
        this.color = color;
        return this;
    }

    public EnumChatFormatting getColor() {
        return color;
    }

    public ChatMessageComponent setBold(Boolean bold) {
        this.bold = bold;
        return this;
    }

    public Boolean isBold() {
        return bold;
    }

    public ChatMessageComponent setItalic(Boolean italic) {
        this.italic = italic;
        return this;
    }

    public Boolean isItalic() {
        return italic;
    }

    public ChatMessageComponent setUnderline(Boolean underline) {
        this.underline = underline;
        return this;
    }

    public Boolean isUnderline() {
        return underline;
    }

    public ChatMessageComponent setObfuscated(Boolean obfuscated) {
        this.obfuscated = obfuscated;
        return this;
    }

    public Boolean isObfuscated() {
        return obfuscated;
    }

    /** Plain text, or the text of the root when it has no translation key. */
    public String getText() {
        return text;
    }

    public String getTranslationKey() {
        return translationKey;
    }

    public List getSubComponents() {
        return subComponents;
    }

    /** Appends sub component with the text (the root takes it when it is still empty). */
    public ChatMessageComponent addText(String text) {
        return appendComponent(rootIsEmpty() ? null : createFromText(text), text);
    }

    public ChatMessageComponent addKey(String key) {
        return addFormatted(key);
    }

    public ChatMessageComponent addFormatted(String key, Object... args) {
        if (rootIsEmpty()) {
            this.translationKey = key;
            this.translationArgs = args;
            return this;
        }
        ChatMessageComponent sub = new ChatMessageComponent();
        sub.translationKey = key;
        sub.translationArgs = args;
        return appendComponent(sub);
    }

    public ChatMessageComponent appendComponent(ChatMessageComponent component) {
        if (component != null) subComponents.add(component);
        return this;
    }

    private ChatMessageComponent appendComponent(ChatMessageComponent sub, String text) {
        if (sub == null) this.text = text;
        else subComponents.add(sub);
        return this;
    }

    private boolean rootIsEmpty() {
        return text == null && translationKey == null;
    }

    @Override
    public String toString() {
        return toStringWithFormatting(false);
    }

    public String toStringWithFormatting(boolean withFormatting) {
        IChatComponent legacy = mcc$toLegacy();
        return withFormatting ? legacy.getFormattedText() : legacy.getUnformattedText();
    }

    public String toJson() {
        return IChatComponent.Serializer.componentToJson(mcc$toLegacy());
    }

    /** The same message as the 1.7.10 shim's component. */
    public IChatComponent mcc$toLegacy() {
        IChatComponent root;
        if (translationKey != null) {
            Object[] args = translationArgs == null ? new Object[0] : translationArgs.clone();
            for (int i = 0; i < args.length; i++) {
                if (args[i] instanceof ChatMessageComponent c) args[i] = c.mcc$toLegacy();
            }
            root = new ChatComponentTranslation(translationKey, args);
        } else {
            root = new ChatComponentText(text == null ? "" : text);
        }
        if (color != null || bold != null || italic != null || underline != null || obfuscated != null) {
            ChatStyle style = new ChatStyle();
            if (color != null) style.setColor(color);
            if (bold != null) style.setBold(bold);
            if (italic != null) style.setItalic(italic);
            if (underline != null) style.setUnderlined(underline);
            if (obfuscated != null) style.setObfuscated(obfuscated);
            root.setChatStyle(style);
        }
        for (ChatMessageComponent sub : subComponents) root.appendSibling(sub.mcc$toLegacy());
        return root;
    }
}
