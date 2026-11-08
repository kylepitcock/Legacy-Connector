package legacyconnector.vb1_7_3.net.minecraft.src.forge;

/** Forge's texture binding hook also selects a custom sheet for TextureFX updates. */
public final class MinecraftForgeClient {
    public static void bindTexture(String path){org.kylepitcock.legacyconnector.vb1_7_3.AnimationsB173.bindSheet(path);}
    public static void bindTexture(String path,int sub){bindTexture(path);}
    public static void unbindTexture(){bindTexture("/terrain.png");}
    public static void preloadTexture(String path){}
}
