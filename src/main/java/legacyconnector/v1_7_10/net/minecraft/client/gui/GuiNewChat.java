package legacyconnector.v1_7_10.net.minecraft.client.gui;

import legacyconnector.v1_7_10.net.minecraft.util.IChatComponent;
import net.minecraft.network.chat.Component;
import org.kylepitcock.legacyconnector.v1_7_10.client.RenderContext;

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

    /** The lines the player sent (oldest first): the modern chat's recent-message list, live. */
    public java.util.List getSentMessages() {
        var gui = net.minecraft.client.Minecraft.getInstance().gui;
        return gui == null ? new java.util.ArrayList() : gui.getChat().getRecentChat();
    }

    /** Draws the chat window into the active GUI (mods that replace the chat screen draw it themselves). */
    public void drawChat(int updateCounter) {
        var gui = RenderContext.gui();
        var mc = net.minecraft.client.Minecraft.getInstance();
        if (gui == null || mc.gui == null) return;
        var window = mc.getWindow();
        int mouseX = (int) (mc.mouseHandler.xpos() * window.getGuiScaledWidth() / Math.max(1, window.getScreenWidth()));
        int mouseY = (int) (mc.mouseHandler.ypos() * window.getGuiScaledHeight() / Math.max(1, window.getScreenHeight()));
        gui.pose().pushPose();
        // 1.7.10 drew from the top of the chat box at the origin the HUD set up (bottom left, 48 pixels up); modern draws at the screen bottom
        gui.pose().translate(0.0F, -(gui.guiHeight() - 48.0F), 0.0F);
        mc.gui.getChat().render(gui, updateCounter, mouseX, mouseY, getChatOpen());
        gui.pose().popPose();
    }

    public void resetScroll() {
        var gui = net.minecraft.client.Minecraft.getInstance().gui;
        if (gui != null) gui.getChat().resetChatScroll();
    }

    public void scroll(int lines) {
        var gui = net.minecraft.client.Minecraft.getInstance().gui;
        if (gui != null) gui.getChat().scrollChat(lines);
    }

    public void refreshChat() {
        var gui = net.minecraft.client.Minecraft.getInstance().gui;
        if (gui != null) gui.getChat().rescaleChat();
    }

    public int getChatWidth() {
        var gui = net.minecraft.client.Minecraft.getInstance().gui;
        return gui == null ? 320 : gui.getChat().getWidth();
    }

    public int getChatHeight() {
        var gui = net.minecraft.client.Minecraft.getInstance().gui;
        return gui == null ? 180 : gui.getChat().getHeight();
    }

    public float getChatScale() {
        var gui = net.minecraft.client.Minecraft.getInstance().gui;
        return gui == null ? 1.0F : (float) gui.getChat().getScale();
    }

    public int getLineCount() {
        var gui = net.minecraft.client.Minecraft.getInstance().gui;
        return gui == null ? 10 : gui.getChat().getLinesPerPage();
    }

    public static int calculateChatboxWidth(float setting) {
        return net.minecraft.client.gui.components.ChatComponent.getWidth(setting);
    }

    public static int calculateChatboxHeight(float setting) {
        return net.minecraft.client.gui.components.ChatComponent.getHeight(setting);
    }
}
