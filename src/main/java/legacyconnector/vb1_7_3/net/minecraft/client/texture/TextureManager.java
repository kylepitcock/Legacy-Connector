package legacyconnector.vb1_7_3.net.minecraft.client.texture;

import java.awt.image.BufferedImage;

import org.kylepitcock.legacyconnector.vb1_7_3.GuiDrawB173;

/**
 * Beta 1.7.3 {@code RenderEngine}: a texture path ({@code /nether/nfurnace.png}, read from the mods' jars) becomes an id that
 * {@link #bindTexture} binds for the next draws. On the client the ids are modern textures; elsewhere 0.
 */
public class TextureManager {
    public static boolean MIPMAP;

    protected TextureManager() {}

    private static final TextureManager INSTANCE = new TextureManager();

    public static TextureManager mcc$instance() {
        return INSTANCE;
    }

    /** MCP {@code getTexture}. */
    public int getTextureId(String path) {
        return GuiDrawB173.textureId(path);
    }

    /** MCP {@code allocateAndSetupTexture}. */
    public int load(BufferedImage image) {
        return GuiDrawB173.load(image);
    }

    public void bindTexture(int id) {
        GuiDrawB173.bind(id);
    }

    public void delete(int id) {}

    public void tick() {}

    public void reload() {}
}
