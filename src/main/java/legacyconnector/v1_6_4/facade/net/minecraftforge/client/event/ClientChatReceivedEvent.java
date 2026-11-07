package legacyconnector.v1_6_4.facade.net.minecraftforge.client.event;

import legacyconnector.v1_7_10.net.minecraft.util.ChatComponentText;

/** Facade of the 1.7.10 shim's {@code ClientChatReceivedEvent} for 1.6.4 mods: the line was a string with formatting codes. Never instantiated. */
public final class ClientChatReceivedEvent {
    private ClientChatReceivedEvent() {}

    /** {@code event.message}: the line with its formatting codes. */
    public static String mcc$get$message(legacyconnector.v1_7_10.net.minecraftforge.client.event.ClientChatReceivedEvent self) {
        return self.message == null ? "" : self.message.getFormattedText();
    }

    /** {@code event.message = text}: replaces the line (null cancels nothing, it becomes empty). */
    public static void mcc$set$message(legacyconnector.v1_7_10.net.minecraftforge.client.event.ClientChatReceivedEvent self, String text) {
        self.message = new ChatComponentText(text == null ? "" : text);
    }
}
