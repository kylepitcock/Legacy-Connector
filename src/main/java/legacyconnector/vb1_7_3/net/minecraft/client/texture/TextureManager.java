package legacyconnector.vb1_7_3.net.minecraft.client.texture;

import java.awt.image.BufferedImage;

import org.kylepitcock.legacyconnector.vb1_7_3.GuiDrawB173;

/**
 * Beta 1.7.3 {@code RenderEngine}: a texture path ({@code /nether/nfurnace.png}, read from the mods' jars) becomes an id that
 * {@link #bindTexture} binds for the next draws. On the client the ids are modern textures; elsewhere 0.
 */
public class TextureManager {
    public static boolean MIPMAP;
    public legacyconnector.vb1_7_3.net.minecraft.client.resource.pack.TexturePacks texturePacks=new legacyconnector.vb1_7_3.net.minecraft.client.resource.pack.TexturePacks();

    protected TextureManager() {}

    private static final TextureManager INSTANCE = new TextureManager();

    public static TextureManager mcc$instance() {
        return INSTANCE;
    }

    /** MCP {@code getTexture}. */
    public int getTextureId(String path) {
        Integer captured=org.kylepitcock.legacyconnector.vb1_7_3.AnimationsB173.textureId(path);
        if(captured!=null)return captured;
        return GuiDrawB173.textureId(path);
    }

    /** MCP {@code allocateAndSetupTexture}. */
    public int load(BufferedImage image) {
        return GuiDrawB173.load(image);
    }

    public void bindTexture(int id) {
        if(org.kylepitcock.legacyconnector.vb1_7_3.AnimationsB173.bindId(id))return;
        GuiDrawB173.bind(id);
    }

    public void delete(int id) {}

    public void tick() {}

    public void reload() {}
    public void addDynamicTexture(legacyconnector.vb1_7_3.net.minecraft.client.render.texture.DynamicTexture effect){org.kylepitcock.legacyconnector.vb1_7_3.AnimationsB173.add(effect);}
}
