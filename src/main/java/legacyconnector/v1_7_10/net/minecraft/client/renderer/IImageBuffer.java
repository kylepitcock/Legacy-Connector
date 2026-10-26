package legacyconnector.v1_7_10.net.minecraft.client.renderer;

import java.awt.image.BufferedImage;

/** Post-processes a downloaded image (player skins: {@link ImageBufferDownload}) and hears when it was set. */
public interface IImageBuffer {
    BufferedImage parseUserSkin(BufferedImage image);

    void func_152634_a();
}
