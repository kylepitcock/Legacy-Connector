package legacyconnector.v1_12_2.net.minecraft.client.resources.data;

/** The "texture" section of a .mcmeta file (blur, clamp). */
public class TextureMetadataSection implements IMetadataSection {
    private final boolean textureBlur;
    private final boolean textureClamp;

    public TextureMetadataSection(boolean blur, boolean clamp) {
        this.textureBlur = blur;
        this.textureClamp = clamp;
    }

    public boolean getTextureBlur() {
        return textureBlur;
    }

    public boolean getTextureClamp() {
        return textureClamp;
    }
}
