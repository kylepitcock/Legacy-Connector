package legacyconnector.v1_7_10.net.minecraft.client.resources.data;

import java.util.List;

/** The "texture" section of a .mcmeta file (blur, clamp, explicit mipmap list). */
public class TextureMetadataSection implements IMetadataSection {
    private final boolean textureBlur;
    private final boolean textureClamp;
    private final List listMipmaps;

    public TextureMetadataSection(boolean blur, boolean clamp, List mipmaps) {
        this.textureBlur = blur;
        this.textureClamp = clamp;
        this.listMipmaps = mipmaps;
    }

    public boolean getTextureBlur() {
        return textureBlur;
    }

    public boolean getTextureClamp() {
        return textureClamp;
    }

    public List getListMipmaps() {
        return listMipmaps;
    }
}
