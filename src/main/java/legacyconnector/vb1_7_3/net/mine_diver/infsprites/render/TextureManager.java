package legacyconnector.vb1_7_3.net.mine_diver.infsprites.render;

/**
 * InfSprites' sprite sheets: Beta had one terrain and one item sheet of 256 sprites; InfSprites numbered more sheets ({@code sprite >> 8})
 * and handed out a texture per sheet. The layer turns every sprite into a native texture with no 256 limit, so every sheet is the
 * original one.
 */
public final class TextureManager {
    private TextureManager() {}

    public static int getTerrain(int sheet) {
        return legacyconnector.vb1_7_3.net.minecraft.client.texture.TextureManager.mcc$instance().getTextureId("/terrain.png");
    }

    public static int getGuiItems(int sheet) {
        return legacyconnector.vb1_7_3.net.minecraft.client.texture.TextureManager.mcc$instance().getTextureId("/gui/items.png");
    }
}
