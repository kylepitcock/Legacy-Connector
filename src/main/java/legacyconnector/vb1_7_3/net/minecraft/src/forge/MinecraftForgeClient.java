package legacyconnector.vb1_7_3.net.minecraft.src.forge;

/** Forge's texture binding hook also selects a custom sheet for TextureFX updates. */
public final class MinecraftForgeClient {
    /** Forge's texture bind: inside an original renderer it binds that renderer's texture, otherwise it names the sheet a texture effect draws into. */
    public static void bindTexture(String path){
        if(org.kylepitcock.legacyconnector.vb1_7_3.RendererContextB173.active()){org.kylepitcock.legacyconnector.vb1_7_3.RendererContextB173.bind(path);return;}
        org.kylepitcock.legacyconnector.vb1_7_3.AnimationsB173.bindSheet(path);
    }
    public static void bindTexture(String path,int sub){bindTexture(path);}
    public static void unbindTexture(){bindTexture("/terrain.png");}
    public static void preloadTexture(String path){}
}
