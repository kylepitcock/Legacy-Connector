package legacyconnector.v1_6_4.cpw.mods.fml.client;

import legacyconnector.v1_6_4.net.minecraft.client.renderer.texturefx.TextureFX;

/** FML 1.4.7's base class for resolution-aware animated tiles (the tile size fields mods compute pixel offsets with). */
public class FMLTextureFX extends TextureFX implements ITextureFX {
    public int tileSizeBase = 16;
    public int tileSizeSquare = 256;
    public int tileSizeMask = 15;
    public int tileSizeSquareMask = 255;
    public boolean errored;

    public FMLTextureFX(int icon) {
        super(icon);
        imageData = new byte[tileSizeSquare * 4]; // Forge called setup() when the engine registered the tile, not here: subclasses' fields do not exist yet
    }

    public void setup() {
        imageData = new byte[tileSizeSquare * 4];
    }

    @Override
    public void onTextureDimensionsUpdate(int width, int height) {
        tileSizeBase = width >> 4;
        tileSizeSquare = tileSizeBase * tileSizeBase;
        tileSizeMask = tileSizeBase - 1;
        tileSizeSquareMask = tileSizeSquare - 1;
        setup();
    }

    @Override
    public void setErrored(boolean errored) {
        this.errored = errored;
    }

    @Override
    public boolean getErrored() {
        return errored;
    }
}
