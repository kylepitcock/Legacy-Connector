package legacyconnector.v1_7_10.net.minecraft.client.gui;

import legacyconnector.v1_7_10.net.minecraft.util.IChatComponent;
import net.minecraft.network.chat.Component;

/** The chat window of the HUD. */
public class GuiNewChat extends Gui {
    public void printChatMessage(IChatComponent message) {
        if (message == null) return;
        var gui = net.minecraft.client.Minecraft.getInstance().gui;
        if (gui == null) {
            // mods report problems while loading, before the HUD exists
            org.slf4j.LoggerFactory.getLogger("LegacyConnector/Chat").info("[chat] {}", message.getUnformattedText());
            return;
        }
        gui.getChat().addMessage(Component.literal(message.getFormattedText()));
    }

    public void printChatMessageWithOptionalDeletion(IChatComponent message, int id) {
        printChatMessage(message);
    }

    public void clearChatMessages() {
        var gui = net.minecraft.client.Minecraft.getInstance().gui;
        if (gui != null) gui.getChat().clearMessages(false);
    }

    public boolean getChatOpen() {
        return net.minecraft.client.Minecraft.getInstance().screen instanceof net.minecraft.client.gui.screens.ChatScreen;
    }

    public void addToSentMessages(String message) {
        var gui = net.minecraft.client.Minecraft.getInstance().gui;
        if (gui != null) gui.getChat().addRecentChat(message);
    }

    public void deleteChatLine(int id) {
    }
}
