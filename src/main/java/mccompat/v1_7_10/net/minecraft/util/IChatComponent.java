package mccompat.v1_7_10.net.minecraft.util;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public interface IChatComponent extends Iterable<IChatComponent> {
    IChatComponent setChatStyle(ChatStyle style);

    ChatStyle getChatStyle();

    IChatComponent appendText(String text);

    IChatComponent appendSibling(IChatComponent component);

    String getUnformattedTextForChat();

    String getUnformattedText();

    String getFormattedText();

    List<IChatComponent> getSiblings();

    IChatComponent createCopy();

    /** Converts the whole legacy component tree to a modern component. */
    MutableComponent mcc$toModern();

    static IChatComponent mcc$fromModern(Component component) {
        return new ChatComponentText(component.getString());
    }
}
