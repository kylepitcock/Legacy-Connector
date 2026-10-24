package legacyconnector.v1_7_10.net.minecraft.util;

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

    /** The JSON form of components (commands, books, packets). */
    class Serializer {
        public static IChatComponent jsonToComponent(String json) {
            Component c = Component.Serializer.fromJson(json, net.minecraft.core.RegistryAccess.EMPTY);
            return mcc$fromModern(c == null ? Component.empty() : c);
        }

        public static String componentToJson(IChatComponent component) {
            return Component.Serializer.toJson(component.mcc$toModern(), net.minecraft.core.RegistryAccess.EMPTY);
        }
    }
}
