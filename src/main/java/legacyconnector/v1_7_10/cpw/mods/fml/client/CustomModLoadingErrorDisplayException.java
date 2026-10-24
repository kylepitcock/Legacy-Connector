package legacyconnector.v1_7_10.cpw.mods.fml.client;

/** Thrown by a mod to show its own error screen when loading fails; the layer reports it like any load failure. */
public class CustomModLoadingErrorDisplayException extends RuntimeException {
    public CustomModLoadingErrorDisplayException() {
    }

    public CustomModLoadingErrorDisplayException(String message, Throwable cause) {
        super(message, cause);
    }

    public void initGui(legacyconnector.v1_7_10.net.minecraft.client.gui.GuiScreen screen, legacyconnector.v1_7_10.net.minecraft.client.gui.FontRenderer font) {
    }

    public void drawScreen(legacyconnector.v1_7_10.net.minecraft.client.gui.GuiScreen screen, legacyconnector.v1_7_10.net.minecraft.client.gui.FontRenderer font,
            int mouseX, int mouseY, float partialTicks) {
    }
}
