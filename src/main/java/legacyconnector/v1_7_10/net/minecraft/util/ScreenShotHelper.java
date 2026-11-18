package legacyconnector.v1_7_10.net.minecraft.util;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

/** Saves screenshots (F2 and the mods that take their own). */
public class ScreenShotHelper {
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss");

    /**
     * Saves the game's frame buffer as {@code screenshots/<date>.png} under {@code gameDirectory} and returns the chat message the game shows
     * ("Saved screenshot as ..."), synchronously as 1.7.10 did. The legacy frame buffer is not the one the modern game draws to, so the width,
     * height and buffer arguments are ignored: the main window is saved at its own size.
     */
    public static IChatComponent saveScreenshot(File gameDirectory, int width, int height, legacyconnector.v1_7_10.net.minecraft.client.shader.Framebuffer buffer) {
        return saveScreenshot(gameDirectory, null, width, height, buffer);
    }

    public static IChatComponent saveScreenshot(File gameDirectory, String name, int width, int height,
            legacyconnector.v1_7_10.net.minecraft.client.shader.Framebuffer buffer) {
        try {
            File dir = new File(gameDirectory, "screenshots");
            dir.mkdirs();
            File file;
            if (name == null) {
                String stamp = DATE_FORMAT.format(new Date());
                int n = 1;
                while ((file = new File(dir, stamp + (n == 1 ? "" : "_" + n) + ".png")).exists()) n++;
            } else {
                file = new File(dir, name);
            }
            try (com.mojang.blaze3d.platform.NativeImage image = net.minecraft.client.Screenshot.takeScreenshot(
                    net.minecraft.client.Minecraft.getInstance().getMainRenderTarget())) {
                image.writeToFile(file);
            }
            ChatComponentText link = new ChatComponentText(file.getName());
            link.getChatStyle().setChatClickEvent(new legacyconnector.v1_7_10.net.minecraft.event.ClickEvent(
                    legacyconnector.v1_7_10.net.minecraft.event.ClickEvent.Action.OPEN_FILE, file.getAbsolutePath()));
            link.getChatStyle().setUnderlined(Boolean.TRUE);
            return new ChatComponentTranslation("screenshot.success", link);
        } catch (Exception e) {
            return new ChatComponentTranslation("screenshot.failure", e.getMessage());
        }
    }
}
