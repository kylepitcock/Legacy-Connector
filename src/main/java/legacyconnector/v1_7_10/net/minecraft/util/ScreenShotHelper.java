package legacyconnector.v1_7_10.net.minecraft.util;

import java.io.File;

/** Saves screenshots (F2 and the mods that take their own). */
public class ScreenShotHelper {
    /**
     * Saves the game's frame buffer as {@code screenshots/<date>.png} under {@code gameDirectory} and returns the chat message the game shows
     * ("Saved screenshot as ..."). The legacy frame buffer is not the one the modern game draws to, so the width, height and buffer arguments are
     * ignored: the main window is saved at its own size.
     */
    public static IChatComponent saveScreenshot(File gameDirectory, int width, int height, legacyconnector.v1_7_10.net.minecraft.client.shader.Framebuffer buffer) {
        return saveScreenshot(gameDirectory, null, width, height, buffer);
    }

    public static IChatComponent saveScreenshot(File gameDirectory, String name, int width, int height,
            legacyconnector.v1_7_10.net.minecraft.client.shader.Framebuffer buffer) {
        var mc = net.minecraft.client.Minecraft.getInstance();
        IChatComponent[] result = new IChatComponent[1];
        net.minecraft.client.Screenshot.grab(gameDirectory, name, mc.getMainRenderTarget(), message -> result[0] = IChatComponent.mcc$fromModern(message));
        return result[0];
    }
}
