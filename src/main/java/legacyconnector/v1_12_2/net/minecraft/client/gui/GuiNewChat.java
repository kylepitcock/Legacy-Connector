package legacyconnector.v1_12_2.net.minecraft.client.gui;

import legacyconnector.v1_12_2.net.minecraft.client.Minecraft;
import legacyconnector.v1_12_2.net.minecraft.util.text.ITextComponent;

/**
 * The chat window as a view over the modern chat. Messages a mod prints with an id replace the earlier message with that id (1.12's
 * {@code printChatMessageWithOptionalDeletion}); the id becomes a message signature of the modern chat, which deletes by signature.
 */
public class GuiNewChat extends Gui {
    public static final GuiNewChat mcc$INSTANCE = new GuiNewChat(null);
    private static final java.util.Map<Integer, net.minecraft.network.chat.MessageSignature> SIGNATURES = new java.util.concurrent.ConcurrentHashMap<>();

    public GuiNewChat(Minecraft mcIn) {
    }

    private static net.minecraft.client.gui.components.ChatComponent chat() {
        return net.minecraft.client.Minecraft.getInstance().gui.getChat();
    }

    private static net.minecraft.network.chat.MessageSignature signature(int id) {
        return SIGNATURES.computeIfAbsent(id, i -> {
            byte[] bytes = new byte[256];
            java.nio.ByteBuffer.wrap(bytes).putLong(0x4C4547414359L).putInt(i); // "LEGACY" + the id: never a real signature
            return new net.minecraft.network.chat.MessageSignature(bytes);
        });
    }

    public void printChatMessage(ITextComponent chatComponent) {
        printChatMessageWithOptionalDeletion(chatComponent, 0);
    }

    public void printChatMessageWithOptionalDeletion(ITextComponent chatComponent, int chatLineId) {
        if (chatComponent == null) return;
        if (chatLineId != 0) {
            deleteChatLine(chatLineId);
            chat().addMessage(chatComponent.mcc$modern(), signature(chatLineId), net.minecraft.client.GuiMessageTag.system());
        } else {
            chat().addMessage(chatComponent.mcc$modern());
        }
    }

    public void deleteChatLine(int id) {
        var sig = SIGNATURES.get(id);
        if (sig != null) chat().deleteMessage(sig);
    }

    public void clearChatMessages(boolean clearSentMsgHistory) {
        chat().clearMessages(clearSentMsgHistory);
    }

    public void refreshChat() {
        chat().rescaleChat();
    }

    public java.util.List<String> getSentMessages() {
        return chat().getRecentChat().stream().toList();
    }

    public void addToSentMessages(String message) {
        chat().addRecentChat(message);
    }

    public void resetScroll() {
        chat().resetChatScroll();
    }

    public void scroll(int amount) {
        chat().scrollChat(amount);
    }

    public boolean getChatOpen() {
        return net.minecraft.client.Minecraft.getInstance().screen instanceof net.minecraft.client.gui.screens.ChatScreen;
    }

    public int getChatWidth() {
        return chat().getWidth();
    }

    public int getChatHeight() {
        return chat().getHeight();
    }

    public float getChatScale() {
        return (float) chat().getScale();
    }

    public int getLineCount() {
        return chat().getLinesPerPage();
    }
}
