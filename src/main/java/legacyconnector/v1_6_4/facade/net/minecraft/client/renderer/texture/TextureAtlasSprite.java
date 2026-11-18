package legacyconnector.v1_6_4.facade.net.minecraft.client.renderer.texture;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import legacyconnector.v1_6_4.net.minecraft.client.renderer.texture.Texture;

/**
 * Facade of the 1.7.10 shim's {@code TextureAtlasSprite} for 1.5.2 mods (their {@code TextureStitched}): the sheet and frame list the stitcher handed a sprite, and
 * {@code init}, {@code readAnimationInfo} and {@code loadTexture}. Calls through {@code super.} reach the {@code mcc$super$} forms (see {@code ShimLinker}).
 * Never instantiated.
 */
public final class TextureAtlasSprite {
    private TextureAtlasSprite() {}

    private static final java.util.Map<legacyconnector.v1_7_10.net.minecraft.client.renderer.texture.TextureAtlasSprite, Object[]> HELD =
            java.util.Collections.synchronizedMap(new java.util.WeakHashMap<>());

    private static Object[] held(legacyconnector.v1_7_10.net.minecraft.client.renderer.texture.TextureAtlasSprite self) {
        return HELD.computeIfAbsent(self, k -> new Object[2]);
    }

    /** {@code textureSheet}: the sheet texture the sprite sits on (null until {@code init}). */
    public static Texture mcc$get$textureSheet(legacyconnector.v1_7_10.net.minecraft.client.renderer.texture.TextureAtlasSprite self) {
        return (Texture) held(self)[0];
    }

    public static void mcc$set$textureSheet(legacyconnector.v1_7_10.net.minecraft.client.renderer.texture.TextureAtlasSprite self, Texture sheet) {
        held(self)[0] = sheet;
    }

    /** {@code textureList}: the frames of the sprite as textures (an empty list until {@code init}, so code that adds to it works). */
    public static List mcc$get$textureList(legacyconnector.v1_7_10.net.minecraft.client.renderer.texture.TextureAtlasSprite self) {
        Object[] h = held(self);
        if (h[1] == null) h[1] = new ArrayList();
        return (List) h[1];
    }

    public static void mcc$set$textureList(legacyconnector.v1_7_10.net.minecraft.client.renderer.texture.TextureAtlasSprite self, List frames) {
        held(self)[1] = frames;
    }

    /**
     * {@code init(sheet, frames, originX, originY, width, height, rotated)}: the stitcher placed the sprite at (originX, originY) of the sheet. Keeps the sheet
     * and the frame list, takes the position and size, and makes the sprite's UV rectangle of the sheet's size.
     */
    public static void init(legacyconnector.v1_7_10.net.minecraft.client.renderer.texture.TextureAtlasSprite self, Texture sheet, List frames, int originX,
            int originY, int width, int height, boolean rotated) {
        mcc$super$init(self, sheet, frames, originX, originY, width, height, rotated);
    }

    public static void mcc$super$init(legacyconnector.v1_7_10.net.minecraft.client.renderer.texture.TextureAtlasSprite self, Texture sheet, List frames, int originX,
            int originY, int width, int height, boolean rotated) {
        Object[] h = held(self);
        h[0] = sheet;
        h[1] = frames == null ? new ArrayList() : frames;
        self.initSprite(sheet == null ? 0 : sheet.getWidth(), sheet == null ? 0 : sheet.getHeight(), originX, originY, rotated);
        self.setIconWidth(width);
        self.setIconHeight(height);
    }

    /**
     * {@code readAnimationInfo(reader)}: the lines of a sprite's ".txt" file (frame indices with a duration, "0*2,1*2"). The reader is consumed as the original did; the
     * animation itself is the host's (a pack's mcmeta), so the timing is not applied.
     */
    public static void readAnimationInfo(legacyconnector.v1_7_10.net.minecraft.client.renderer.texture.TextureAtlasSprite self, java.io.BufferedReader reader) {
        mcc$super$readAnimationInfo(self, reader);
    }

    public static void mcc$super$readAnimationInfo(legacyconnector.v1_7_10.net.minecraft.client.renderer.texture.TextureAtlasSprite self, java.io.BufferedReader reader) {
        if (reader == null) return;
        try {
            while (reader.readLine() != null) {
                // consumed: see above
            }
        } catch (java.io.IOException ignored) {
            // an unreadable animation file is no animation
        }
    }

    /**
     * Forge's {@code loadTexture(manager, pack, name, file, image, frames)}: a sprite that loads its own pixels returns true; the default (false) leaves the loading to the
     * stitcher. Nothing in the layer calls it (the atlas is stitched by the host), so a sprite class's own loader does not run; as a default it answers false.
     */
    public static boolean loadTexture(legacyconnector.v1_7_10.net.minecraft.client.renderer.texture.TextureAtlasSprite self,
            legacyconnector.v1_7_10.net.minecraft.client.renderer.texture.TextureManager manager, legacyconnector.v1_6_4.net.minecraft.client.texturepacks.ITexturePack pack,
            String name, String file, BufferedImage image, ArrayList frames) {
        return false;
    }

    public static boolean mcc$super$loadTexture(legacyconnector.v1_7_10.net.minecraft.client.renderer.texture.TextureAtlasSprite self,
            legacyconnector.v1_7_10.net.minecraft.client.renderer.texture.TextureManager manager, legacyconnector.v1_6_4.net.minecraft.client.texturepacks.ITexturePack pack,
            String name, String file, BufferedImage image, ArrayList frames) {
        return false;
    }
}
