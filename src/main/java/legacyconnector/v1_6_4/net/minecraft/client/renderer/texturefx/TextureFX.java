package legacyconnector.v1_6_4.net.minecraft.client.renderer.texturefx;

import legacyconnector.v1_7_10.net.minecraft.client.renderer.texture.TextureManager;

/**
 * Minecraft 1.4.7's animated tile: the game called {@link #onTick} every tick and uploaded {@link #imageData} (RGBA, tile size squared times four bytes)
 * to the tile {@link #iconIndex} of the sheet {@link #bindImage} binds. {@code Animations147} does the same with the sprite of the cut sheet.
 */
public class TextureFX {
    public byte[] imageData = new byte[1024];
    public int iconIndex;
    public boolean anaglyphEnabled;
    public int textureId;
    public int tileSize = 1;
    public int tileImage;

    public TextureFX(int iconIndex) {
        this.iconIndex = iconIndex;
    }

    public void onTick() {
    }

    /** Binds the sheet the tile is on ({@code /terrain.png} by default). */
    public void bindImage(TextureManager engine) {
        engine.bindTexture("/terrain.png");
    }
}
