package legacyconnector.v1_7_10.net.minecraft.client.gui;

/** 1.7.10's new-world screen; displaying it opens the modern world creation screen (see ClientGui). */
public class GuiCreateWorld extends GuiScreen {
    protected GuiScreen field_146332_f;

    public GuiCreateWorld(GuiScreen parent) {
        this.field_146332_f = parent;
    }

    public GuiScreen mcc$parent() {
        return field_146332_f;
    }
}
