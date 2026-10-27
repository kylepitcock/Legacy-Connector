package legacyconnector.v1_12_2.net.minecraft.client.gui;

/** The singleplayer world list; displaying it shows the modern world list with the open screen as its parent. */
public class GuiWorldSelection extends GuiScreen {
    protected GuiScreen prevScreen;

    public GuiWorldSelection(GuiScreen screenIn) {
        this.prevScreen = screenIn;
    }
}
