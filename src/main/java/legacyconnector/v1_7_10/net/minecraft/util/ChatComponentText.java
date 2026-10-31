package legacyconnector.v1_7_10.net.minecraft.util;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public class ChatComponentText extends ChatComponentStyle {
    private final String text;

    public ChatComponentText(String text) {
        this.text = text;
    }

    public String getChatComponentText_TextValue() {
        return text;
    }

    @Override
    public String getUnformattedTextForChat() {
        return text;
    }

    @Override
    public IChatComponent createCopy() {
        ChatComponentText copy = new ChatComponentText(text);
        copy.setChatStyle(getChatStyle().createShallowCopy());
        for (IChatComponent s : getSiblings()) copy.appendSibling(s.createCopy());
        return copy;
    }

    @Override
    protected MutableComponent mcc$self() {
        // DragonAPI font keys select a custom font only legacy drawString knows; the modern chat drops them (LegacyText)
        return Component.literal(org.kylepitcock.legacyconnector.v1_7_10.bridge.LegacyText.strip(text));
    }
}
