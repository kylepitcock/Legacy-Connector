package legacyconnector.v1_7_10.net.minecraft.client.gui;

import legacyconnector.v1_7_10.net.minecraft.util.IChatComponent;
import net.minecraft.network.chat.Component;

/** The chat window of the HUD. */
public class GuiNewChat extends Gui {
    public void printChatMessage(IChatComponent message) {
        if (message == null) return;
        net.minecraft.client.Minecraft.getInstance().gui.getChat().addMessage(Component.literal(message.getFormattedText()));
    }

    public void printChatMessageWithOptionalDeletion(IChatComponent message, int id) {
        printChatMessage(message);
    }

    public void clearChatMessages() {
        net.minecraft.client.Minecraft.getInstance().gui.getChat().clearMessages(false);
    }

    public boolean getChatOpen() {
        return net.minecraft.client.Minecraft.getInstance().screen instanceof net.minecraft.client.gui.screens.ChatScreen;
    }
}
