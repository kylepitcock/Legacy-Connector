package legacyconnector.v1_12_2.net.minecraft.client.gui;

/** The chat input; displaying it shows the modern chat screen with the default text. */
public class GuiChat extends GuiScreen {
    protected GuiTextField inputField;
    private String defaultInputFieldText = "";

    public GuiChat() {
    }

    public GuiChat(String defaultText) {
        this.defaultInputFieldText = defaultText == null ? "" : defaultText;
    }

    public String mcc$defaultText() {
        return defaultInputFieldText;
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
