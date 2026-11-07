package legacyconnector.v1_6_4.facade.net.minecraftforge.event;

import legacyconnector.v1_6_4.net.minecraft.util.ChatMessageComponent;
import legacyconnector.v1_7_10.net.minecraft.util.ChatComponentTranslation;

/** Facade of the 1.7.10 shim's {@code ServerChatEvent} for 1.4.7 to 1.6.4 mods: {@code line}, and the component as a {@code ChatMessageComponent}. Never instantiated. */
public final class ServerChatEvent {
    private ServerChatEvent() {}

    /** 1.4.7 and 1.5.2 {@code event.line}: the text the player typed. */
    public static String mcc$get$line(legacyconnector.v1_7_10.net.minecraftforge.event.ServerChatEvent self) {
        return self.message;
    }

    /** 1.6.4 {@code event.component}: the line as it will be broadcast ({@code <player> text}). */
    public static ChatMessageComponent mcc$get$component(legacyconnector.v1_7_10.net.minecraftforge.event.ServerChatEvent self) {
        return ChatMessageComponent.createFromText(self.component == null ? self.message : self.component.getFormattedText());
    }

    /** {@code event.component = c}: the broadcast line is the component, shown as the chat line of the player. */
    public static void mcc$set$component(legacyconnector.v1_7_10.net.minecraftforge.event.ServerChatEvent self, ChatMessageComponent component) {
        if (component == null) return;
        self.component = new ChatComponentTranslation("chat.type.text", self.username, component.mcc$toLegacy());
    }
}
