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

    /**
     * FML 1.4.7 {@code loadImageFromTexturePack(renderEngine, path)}: the image a texture pack path names, read the way the pack did: a mod's class path file
     * first (its own sprite sheets and animation frames), then the loaded resource packs under the location 1.4.7's path maps to (vanilla and mod assets
     * such as {@code /gui/items.png} or {@code /mods/<id>/textures/...}). Null for a path no pack has, as FML answered when the stream was missing.
     */
    public java.awt.image.BufferedImage loadImageFromTexturePack(legacyconnector.v1_7_10.net.minecraft.client.renderer.texture.TextureManager engine, String path) throws java.io.IOException {
        if (path == null) return null;
        try (java.io.InputStream in = openImage(path)) {
            return in == null ? null : javax.imageio.ImageIO.read(in);
        }
    }

    private static java.io.InputStream openImage(String path) throws java.io.IOException {
        java.io.InputStream stream = org.kylepitcock.legacyconnector.v1_7_10.client.ClasspathTextures.open(path);
        if (stream != null) return stream;
        var location = org.kylepitcock.legacyconnector.v1_6_4.bridge.Textures152.location(path);
        var minecraft = legacyconnector.v1_7_10.net.minecraft.client.Minecraft.getMinecraft();
        if (location == null || minecraft == null || minecraft.getResourceManager() == null) return null;
        try {
            return minecraft.getResourceManager().getResource(location).getInputStream();
        } catch (java.io.IOException | RuntimeException e) {
            return null;
        }
    }

    public void addAnimation(TextureFX anim) {
        Animations147.register(anim);
    }

    public void removeAnimation(TextureFX anim) {
        Animations147.unregister(anim);
    }
}
