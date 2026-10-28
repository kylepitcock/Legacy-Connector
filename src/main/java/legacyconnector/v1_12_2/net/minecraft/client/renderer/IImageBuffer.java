package legacyconnector.v1_12_2.net.minecraft.client.renderer;

import java.awt.image.BufferedImage;

/** 1.12.2 hook over a downloaded image (skins): reshape it before upload and be told when it is available. */
public interface IImageBuffer {
    BufferedImage parseUserSkin(BufferedImage image);

    void skinAvailable();
}
