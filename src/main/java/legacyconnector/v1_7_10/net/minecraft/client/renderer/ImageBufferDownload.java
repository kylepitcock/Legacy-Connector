package legacyconnector.v1_7_10.net.minecraft.client.renderer;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;

/** 1.7.10's skin fixer: a downloaded skin becomes a 64x32 image, opaque except the hat layer (cleared unless it uses transparency). */
public class ImageBufferDownload implements IImageBuffer {
    private int[] imageData;
    private int imageWidth;
    private int imageHeight;

    @Override
    public BufferedImage parseUserSkin(BufferedImage image) {
        if (image == null) return null;
        imageWidth = 64;
        imageHeight = 32;
        BufferedImage out = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics g = out.getGraphics();
        g.drawImage(image, 0, 0, null);
        g.dispose();
        imageData = ((DataBufferInt) out.getRaster().getDataBuffer()).getData();
        setAreaOpaque(0, 0, 32, 16);
        setAreaTransparent(32, 0, 64, 32);
        setAreaOpaque(0, 16, 64, 32);
        return out;
    }

    @Override
    public void func_152634_a() {
    }

    private void setAreaTransparent(int x1, int y1, int x2, int y2) {
        if (hasTransparency(x1, y1, x2, y2)) return;
        for (int x = x1; x < x2; x++) {
            for (int y = y1; y < y2; y++) imageData[x + y * imageWidth] &= 0xFFFFFF;
        }
    }

    private void setAreaOpaque(int x1, int y1, int x2, int y2) {
        for (int x = x1; x < x2; x++) {
            for (int y = y1; y < y2; y++) imageData[x + y * imageWidth] |= 0xFF000000;
        }
    }

    private boolean hasTransparency(int x1, int y1, int x2, int y2) {
        for (int x = x1; x < x2; x++) {
            for (int y = y1; y < y2; y++) {
                if ((imageData[x + y * imageWidth] >> 24 & 255) < 128) return true;
            }
        }
        return false;
    }
}
