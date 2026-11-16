package legacyconnector.v1_6_4.net.minecraft.src;

import java.awt.image.BufferedImage;
import legacyconnector.v1_6_4.net.minecraft.client.renderer.texturefx.TextureFX;
import legacyconnector.v1_7_10.net.minecraft.client.renderer.texture.TextureManager;

/**
 * ModLoader's still tile ({@code ModTextureStatic}): an image copied into tile {@code icon} of the sheet {@code target} (0 terrain, 1 items) once and kept.
 * {@code tileSize} is the number of tiles it spans per side ({@code ModLoader.addAnimation} callers pass 1 for a normal tile).
 */
public class ModTextureStatic extends TextureFX {
    public ModTextureStatic(int icon, int target, BufferedImage image) {
        this(icon, 1, target, image);
    }

    public ModTextureStatic(int icon, int tileSize, int target, BufferedImage image) {
        super(icon);
        this.tileImage = target;
        this.tileSize = tileSize;
        int w = image.getWidth(), h = image.getHeight();
        imageData = new byte[w * h * 4];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int argb = image.getRGB(x, y);
                int i = (y * w + x) * 4;
                imageData[i] = (byte) (argb >> 16);
                imageData[i + 1] = (byte) (argb >> 8);
                imageData[i + 2] = (byte) argb;
                imageData[i + 3] = (byte) (argb >>> 24);
            }
        }
    }

    /** Recomputes the pixels (ModLoader did this when the anaglyph setting changed; the image is kept as it is here). */
    public void update() {}

    @Override
    public void onTick() {}

    @Override
    public void bindImage(TextureManager engine) {
        engine.bindTexture(tileImage == 1 ? "/gui/items.png" : "/terrain.png");
    }

    /** Doubles an image's size (ModLoader used it when the texture pack had 32x tiles). */
    public static BufferedImage scale2x(BufferedImage source) {
        BufferedImage out = new BufferedImage(source.getWidth() * 2, source.getHeight() * 2, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < out.getHeight(); y++) {
            for (int x = 0; x < out.getWidth(); x++) out.setRGB(x, y, source.getRGB(x / 2, y / 2));
        }
        return out;
    }
}
