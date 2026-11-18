package legacyconnector.v1_6_4.facade.net.minecraft.client.renderer.texture;

import legacyconnector.v1_7_10.net.minecraft.client.renderer.texture.TextureAtlasSprite;

/** Facade of the 1.7.10 shim's {@code TextureMap} for 1.5.2 mods. See {@code Hierarchy.facadeOf}. Never instantiated. */
public final class TextureMap {
    private TextureMap() {}

    private static TextureAtlasSprite missing;

    /** The sprite of the atlas's "missingno" tile (the host's own purple and black square), shared by every sheet. */
    static synchronized TextureAtlasSprite missingIcon() {
        if (missing == null) {
            missing = TextureAtlasSprite.makeAtlasSprite("minecraft:missingno");
            missing.mcc$legacyName = "missingno";
        }
        return missing;
    }

    /** 1.5.2 {@code TextureMap.getMissingIcon()}: the tile mods draw when a name has no icon. */
    public static legacyconnector.v1_7_10.net.minecraft.util.IIcon getMissingIcon(legacyconnector.v1_7_10.net.minecraft.client.renderer.texture.TextureMap self) {
        return missingIcon();
    }
}
