package legacyconnector.v1_7_10.net.minecraft.client.gui;

/** 1.7.10's resource pack screen; displaying it opens the modern pack selection screen (see ClientGui). */
public class GuiScreenResourcePacks extends GuiScreen {
    protected GuiScreen field_146965_f;

    public GuiScreenResourcePacks(GuiScreen parent) {
        this.field_146965_f = parent;
    }

    public GuiScreen mcc$parent() {
        return field_146965_f;
    }
}
