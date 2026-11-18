package legacyconnector.v1_6_4.facade.net.minecraft.client.gui;

/** Facade of the 1.7.10 shim's {@code GuiNewChat} for 1.4.7 and 1.5.2 mods. See {@code Hierarchy.facadeOf}. Never instantiated. */
public final class GuiNewChat {
    private GuiNewChat() {}

    /** {@code printChatMessageWithOptionalDeletion(text, id)}: a chat line that replaces an earlier one with the same id (1.7.10 takes a chat component). */
    public static void printChatMessageWithOptionalDeletion(legacyconnector.v1_7_10.net.minecraft.client.gui.GuiNewChat self, String text, int id) {
        self.printChatMessageWithOptionalDeletion(new legacyconnector.v1_7_10.net.minecraft.util.ChatComponentText(text == null ? "" : text), id);
    }
}
