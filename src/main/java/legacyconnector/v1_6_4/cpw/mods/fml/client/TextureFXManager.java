package legacyconnector.v1_6_4.cpw.mods.fml.client;

import legacyconnector.v1_6_4.net.minecraft.client.renderer.texturefx.TextureFX;
import org.kylepitcock.legacyconnector.v1_6_4.bridge.Animations147;

/** FML 1.4.7's registry of animated tiles. */
public class TextureFXManager {
    private static final TextureFXManager INSTANCE = new TextureFXManager();

    public static TextureFXManager instance() {
        return INSTANCE;
    }

    /**
     * The size of the texture at a GL id, as FML answered it: every sheet this layer hands out is made of 16 by 16 tiles, so animations
     * (which draw a tile each) get the 16 by 16 of a vanilla sheet.
     */
    public java.awt.Dimension getTextureDimensions(int textureId) {
        return new java.awt.Dimension(16, 16);
    }

    public void addAnimation(TextureFX anim) {
        Animations147.register(anim);
    }

    public void removeAnimation(TextureFX anim) {
        Animations147.unregister(anim);
    }
}
