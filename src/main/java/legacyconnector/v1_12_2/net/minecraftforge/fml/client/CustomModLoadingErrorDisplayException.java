package legacyconnector.v1_12_2.net.minecraftforge.fml.client;

import legacyconnector.v1_12_2.net.minecraft.client.gui.FontRenderer;
import legacyconnector.v1_12_2.net.minecraft.client.gui.GuiErrorScreen;

/**
 * Thrown by a mod (from preInit, usually) to show its own error screen instead of FML's. The modern game reports it as a load failure
 * of the mod with this message; the screen hooks are not called.
 */
public class CustomModLoadingErrorDisplayException extends RuntimeException {
    public CustomModLoadingErrorDisplayException() {
    }

    public CustomModLoadingErrorDisplayException(String message, Throwable cause) {
        super(message, cause);
    }

    public void initGui(GuiErrorScreen errorScreen, FontRenderer fontRenderer) {
    }

    public void drawScreen(GuiErrorScreen errorScreen, FontRenderer fontRenderer, int mouseRelX, int mouseRelY, float tickTime) {
    }
}
