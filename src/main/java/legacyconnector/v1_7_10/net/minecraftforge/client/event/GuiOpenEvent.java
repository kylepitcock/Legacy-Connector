package legacyconnector.v1_7_10.net.minecraftforge.client.event;

import legacyconnector.v1_7_10.cpw.mods.fml.common.eventhandler.Cancelable;
import legacyconnector.v1_7_10.cpw.mods.fml.common.eventhandler.Event;
import legacyconnector.v1_7_10.net.minecraft.client.gui.GuiScreen;

/** Fired when a screen is about to open; a listener may replace {@link #gui} (null closes the screen) or cancel. */
@Cancelable
public class GuiOpenEvent extends Event {
    public GuiScreen gui;

    public GuiOpenEvent(GuiScreen gui) {
        this.gui = gui;
    }
}
