package legacyconnector.v1_12_2.net.minecraft.client.renderer;

import java.awt.image.BufferedImage;

/** 1.12.2 skin image processing: a 64x32 legacy skin is widened to 64x64 (the left limbs mirrored from the right) and the hidden parts made opaque. */
public class ImageBufferDownload implements IImageBuffer {
    private int[] imageData;
    private int imageWidth;
    private int imageHeight;

    @Override
    public BufferedImage parseUserSkin(BufferedImage image) {
        if (image == null) return null;
        this.imageWidth = 64;
        this.imageHeight = 64;
        BufferedImage out = new BufferedImage(64, 64, 2);
        java.awt.Graphics graphics = out.getGraphics();
        graphics.drawImage(image, 0, 0, null);
        boolean legacy = image.getHeight() == 32;
        if (legacy) {
            graphics.setColor(new java.awt.Color(0, 0, 0, 0));
            graphics.fillRect(0, 32, 64, 32);
            graphics.drawImage(out, 24, 48, 20, 52, 4, 16, 8, 20, null);
            graphics.drawImage(out, 28, 48, 24, 52, 8, 16, 12, 20, null);
            graphics.drawImage(out, 20, 52, 16, 64, 8, 20, 12, 32, null);
            graphics.drawImage(out, 24, 52, 20, 64, 4, 20, 8, 32, null);
            graphics.drawImage(out, 28, 52, 24, 64, 0, 20, 4, 32, null);
            graphics.drawImage(out, 32, 52, 28, 64, 12, 20, 16, 32, null);
            graphics.drawImage(out, 40, 48, 36, 52, 44, 16, 48, 20, null);
            graphics.drawImage(out, 44, 48, 40, 52, 48, 16, 52, 20, null);
            graphics.drawImage(out, 36, 52, 32, 64, 48, 20, 52, 32, null);
            graphics.drawImage(out, 40, 52, 36, 64, 44, 20, 48, 32, null);
            graphics.drawImage(out, 44, 52, 40, 64, 40, 20, 44, 32, null);
            graphics.drawImage(out, 48, 52, 44, 64, 52, 20, 56, 32, null);
        }
        graphics.dispose();
        this.imageData = ((java.awt.image.DataBufferInt) out.getRaster().getDataBuffer()).getData();
        setAreaOpaque(0, 0, 32, 16);
        if (legacy) setAreaTransparent(32, 0, 64, 32);
        setAreaOpaque(0, 16, 64, 32);
        setAreaOpaque(16, 48, 48, 64);
        return out;
    }

    @Override
    public void skinAvailable() {
    }

    private void setAreaTransparent(int x, int y, int width, int height) {
        for (int i = x; i < width; ++i) {
            for (int j = y; j < height; ++j) {
                int k = imageData[i + j * imageWidth];
                if ((k >> 24 & 255) < 128) return;
            }
        }
        for (int l = x; l < width; ++l) {
            for (int i1 = y; i1 < height; ++i1) imageData[l + i1 * imageWidth] &= 16777215;
        }
    }

    private void setAreaOpaque(int x, int y, int width, int height) {
        for (int i = x; i < width; ++i) {
            for (int j = y; j < height; ++j) imageData[i + j * imageWidth] |= -16777216;
        }
    }
}
