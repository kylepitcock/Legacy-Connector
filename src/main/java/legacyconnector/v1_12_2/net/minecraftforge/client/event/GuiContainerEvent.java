package legacyconnector.v1_12_2.net.minecraftforge.client.event;

import legacyconnector.v1_12_2.net.minecraft.client.gui.inventory.GuiContainer;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Event;

/** Events of 1.12 container screens. */
public class GuiContainerEvent extends Event {
    private final GuiContainer guiContainer;

    public GuiContainerEvent(GuiContainer guiContainer) {
        this.guiContainer = guiContainer;
    }

    public GuiContainer getGuiContainer() {
        return guiContainer;
    }

    /** After the foreground layer: drawing is relative to the container's corner. */
    public static class DrawForeground extends GuiContainerEvent {
        private final int mouseX;
        private final int mouseY;

        public DrawForeground(GuiContainer guiContainer, int mouseX, int mouseY) {
            super(guiContainer);
            this.mouseX = mouseX;
            this.mouseY = mouseY;
        }

        public int getMouseX() {
            return mouseX;
        }

        public int getMouseY() {
            return mouseY;
        }
    }
}
