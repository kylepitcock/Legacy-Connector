package legacyconnector.v1_12_2.net.minecraft.util;

import java.awt.image.BufferedImage;
import java.io.File;
import legacyconnector.v1_12_2.net.minecraft.client.shader.Framebuffer;
import legacyconnector.v1_12_2.net.minecraft.util.text.ITextComponent;
import legacyconnector.v1_12_2.net.minecraft.util.text.TextComponentString;

/** Screenshots of a framebuffer (the game's main one when null): read back from the modern render target. Render thread only. */
public class ScreenShotHelper {
    private ScreenShotHelper() {
    }

    private static com.mojang.blaze3d.pipeline.RenderTarget mcc$target(Framebuffer buffer) {
        var target = buffer == null ? null : buffer.mcc$target();
        return target != null ? target : net.minecraft.client.Minecraft.getInstance().getMainRenderTarget();
    }

    /** Saves a screenshot under {@code screenshots/} of the game directory (the name is the date when null); the chat message to show. */
    public static ITextComponent saveScreenshot(File gameDirectory, int width, int height, Framebuffer buffer) {
        return saveScreenshot(gameDirectory, null, width, height, buffer);
    }

    public static ITextComponent saveScreenshot(File gameDirectory, String screenshotName, int width, int height, Framebuffer buffer) {
        ITextComponent[] message = {new TextComponentString("")};
        net.minecraft.client.Screenshot.grab(gameDirectory, screenshotName, mcc$target(buffer),
                component -> message[0] = new TextComponentString(component.getString()));
        return message[0];
    }

    /** The framebuffer's pixels as an image (alpha dropped, as 1.12's was opaque). */
    public static BufferedImage createScreenshot(int width, int height, Framebuffer framebufferIn) {
        try (var image = net.minecraft.client.Screenshot.takeScreenshot(mcc$target(framebufferIn))) {
            int w = image.getWidth();
            int h = image.getHeight();
            BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int abgr = image.getPixelRGBA(x, y);
                    int r = abgr & 0xFF;
                    int g = abgr >> 8 & 0xFF;
                    int b = abgr >> 16 & 0xFF;
                    out.setRGB(x, y, r << 16 | g << 8 | b);
                }
            }
            return out;
        }
    }
}
