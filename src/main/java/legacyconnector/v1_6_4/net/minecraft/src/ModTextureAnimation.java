package legacyconnector.v1_6_4.net.minecraft.src;

import java.awt.image.BufferedImage;
import legacyconnector.v1_6_4.net.minecraft.client.renderer.texturefx.TextureFX;
import legacyconnector.v1_7_10.net.minecraft.client.renderer.texture.TextureManager;

/**
 * ModLoader's animated tile: the frames of a vertical strip (width squared per frame) shown one per {@code tickRate} ticks on tile {@code icon} of the
 * sheet {@code target} (0 terrain, 1 items).
 */
public class ModTextureAnimation extends TextureFX {
    private final int tickRate;
    private final byte[][] images;
    private int index;
    private int ticks;

    public ModTextureAnimation(int icon, int target, BufferedImage image, int tickRate) {
        this(icon, 1, target, image, tickRate);
    }

    public ModTextureAnimation(int icon, int tileSize, int target, BufferedImage image, int tickRate) {
        super(icon);
        this.tileImage = target;
        this.tileSize = tileSize;
        this.tickRate = tickRate;
        this.ticks = tickRate;
        int size = image.getWidth();
        int frames = image.getHeight() / size;
        if (frames < 1) throw new IllegalArgumentException("source has no complete images");
        images = new byte[frames][size * size * 4];
        for (int f = 0; f < frames; f++) {
            for (int y = 0; y < size; y++) {
                for (int x = 0; x < size; x++) {
                    int argb = image.getRGB(x, y + f * size);
                    int i = (y * size + x) * 4;
                    images[f][i] = (byte) (argb >> 16);
                    images[f][i + 1] = (byte) (argb >> 8);
                    images[f][i + 2] = (byte) argb;
                    images[f][i + 3] = (byte) (argb >>> 24);
                }
            }
        }
        imageData = images[0].clone();
    }

    @Override
    public void onTick() {
        if (ticks >= tickRate) {
            index = (index + 1) % images.length;
            System.arraycopy(images[index], 0, imageData, 0, imageData.length);
            ticks = 0;
        }
        ticks++;
    }

    @Override
    public void bindImage(TextureManager engine) {
        engine.bindTexture(tileImage == 1 ? "/gui/items.png" : "/terrain.png");
    }
}
