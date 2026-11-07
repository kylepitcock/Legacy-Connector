package legacyconnector.v1_6_4.net.minecraft.client.texturepacks;

import java.io.InputStream;

/** Minecraft 1.4.7's texture pack: mods read their own images through {@link #getResourceAsStream}. */
public interface ITexturePack {
    InputStream getResourceAsStream(String path);

    String getTexturePackFileName();

    String getFirstDescriptionLine();

    String getSecondDescriptionLine();
}
