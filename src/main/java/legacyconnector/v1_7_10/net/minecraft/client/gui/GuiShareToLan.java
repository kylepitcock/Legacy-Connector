package legacyconnector.v1_7_10.net.minecraft.client.gui;

/** 1.7.10's "Open to LAN" screen. Displaying one opens the modern one over the same parent (see ClientGui). */
public class GuiShareToLan extends GuiScreen {
    private final GuiScreen parentScreen;

    public GuiShareToLan(GuiScreen parent) {
        this.parentScreen = parent;
    }

    public GuiScreen mcc$parent() {
        return parentScreen;
    }
}
