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

    /** The path each id was last handed out for: ids are per sink (a HUD capture numbers its own), so a mod's {@code textures.put(path, id)} is read back as a path. */
    private static final java.util.Map<Integer,String> RECENT=new java.util.concurrent.ConcurrentHashMap<>();
    private static final java.util.Map<String,String> OVERRIDES=new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * Beta's private name-to-id table of the render engine (MCP {@code textureMap}), which mods read and write through reflection to swap a
     * texture (a texture-pack-like override of {@code /terrain/moon.png}). Reads give the default id of a path; a write of another path's
     * id makes that path stand in for the key, which {@link #override} reports to the renderers the layer owns.
     */
    public final java.util.HashMap<String,Integer> textures=new java.util.HashMap<>(){
        @Override public Integer get(Object key){
            Integer value=super.get(key);
            if(value==null&&key instanceof String path){value=TextureManager.this.getTextureId(path);super.put(path,value);}
            return value;
        }
        @Override public Integer put(String key,Integer id){
            String path=id==null?null:RECENT.get(id);
            String before=OVERRIDES.get(key);
            if(path==null||path.equals(key))OVERRIDES.remove(key);else OVERRIDES.put(key,path);
            String after=OVERRIDES.get(key);
            var listener=LISTENER;if(listener!=null&&!java.util.Objects.equals(before,after))listener.accept(key,after);
            return super.put(key,id);
        }
    };

    /** Told when an override appears, changes or goes (key, path or null): a server passes it on to its remote players. */
    public static volatile java.util.function.BiConsumer<String,String> LISTENER;
    /** An override a remote server reported: set without the listener. */
    public static void mcc$remoteOverride(String key,String path){if(path==null||path.isEmpty())OVERRIDES.remove(key);else OVERRIDES.put(key,path);}

    /** The path of the texture standing in for {@code key} (a mod wrote another id under it), or null. */
    public static String override(String key){return OVERRIDES.get(key);}
    /** The overrides in force (key to path). */
    public static java.util.Map<String,String> overrides(){return java.util.Map.copyOf(OVERRIDES);}

    private static final TextureManager INSTANCE = new TextureManager();

    public static TextureManager mcc$instance() {
        return INSTANCE;
    }

    /** MCP {@code getTexture}. */
    public int getTextureId(String path) {
        int id=resolve(path);
        RECENT.put(id,path);
        return id;
    }

    private int resolve(String path) {
        if(org.kylepitcock.legacyconnector.vb1_7_3.RendererContextB173.active())return org.kylepitcock.legacyconnector.vb1_7_3.RendererContextB173.textureId(path);
        Integer captured=org.kylepitcock.legacyconnector.vb1_7_3.AnimationsB173.textureId(path);
        if(captured!=null)return captured;
        return GuiDrawB173.textureId(path);
    }

    /** MCP {@code allocateAndSetupTexture}. */
    public int load(BufferedImage image) {
        return GuiDrawB173.load(image);
    }

    public void bindTexture(int id) {
        if(org.kylepitcock.legacyconnector.vb1_7_3.RendererContextB173.active()){org.kylepitcock.legacyconnector.vb1_7_3.RendererContextB173.bind(id);return;}
        if(org.kylepitcock.legacyconnector.vb1_7_3.AnimationsB173.bindId(id))return;
        GuiDrawB173.bind(id);
    }

    public void delete(int id) {}

    public void tick() {}

    public void reload() {}
    public void addDynamicTexture(legacyconnector.vb1_7_3.net.minecraft.client.render.texture.DynamicTexture effect){org.kylepitcock.legacyconnector.vb1_7_3.AnimationsB173.add(effect);}
}
