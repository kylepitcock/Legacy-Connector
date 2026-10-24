package legacyconnector.v1_7_10.net.minecraft.client.gui;

/** The in-game HUD ({@code Minecraft.ingameGUI}); mods mostly reach the chat window through it. */
public class GuiIngame extends Gui {
    private final GuiNewChat chat = new GuiNewChat();

    public GuiNewChat getChatGUI() {
        return chat;
    }

    public int getUpdateCounter() {
        return net.minecraft.client.Minecraft.getInstance().gui.getGuiTicks();
    }

    public void setRecordPlayingMessage(String message) {
        net.minecraft.client.Minecraft.getInstance().gui.setOverlayMessage(net.minecraft.network.chat.Component.literal(message), true);
    }
}
