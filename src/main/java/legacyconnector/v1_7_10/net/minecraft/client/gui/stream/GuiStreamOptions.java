package legacyconnector.v1_7_10.net.minecraft.client.gui.stream;

import legacyconnector.v1_7_10.net.minecraft.client.gui.GuiScreen;
import legacyconnector.v1_7_10.net.minecraft.client.settings.GameSettings;

/** 1.7.10's broadcast settings. Broadcasting is gone; displaying it shows {@link GuiStreamUnavailable} (see ClientGui). */
public class GuiStreamOptions extends GuiScreen {
    protected final GuiScreen field_152317_g;
    protected final GameSettings field_152318_h;

    public GuiStreamOptions(GuiScreen parent, GameSettings settings) {
        this.field_152317_g = parent;
        this.field_152318_h = settings;
    }

    public GuiScreen mcc$parent() {
        return field_152317_g;
    }
}
