package legacyconnector.v1_12_2.net.minecraftforge.client.event;

import legacyconnector.v1_12_2.net.minecraft.client.gui.GuiScreen;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Cancelable;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Event;

/** A screen is about to open (null: the open one closes). Handlers may show another screen instead, or cancel the change. */
@Cancelable
public class GuiOpenEvent extends Event {
    private GuiScreen gui;

    public GuiOpenEvent(GuiScreen gui) {
        this.gui = gui;
    }

    public GuiScreen getGui() {
        return gui;
    }

    public void setGui(GuiScreen gui) {
        this.gui = gui;
    }
}
