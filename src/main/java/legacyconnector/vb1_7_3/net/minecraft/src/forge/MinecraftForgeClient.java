package legacyconnector.vb1_7_3.net.minecraft.src.forge;

/** Forge's texture binding hook also selects a custom sheet for TextureFX updates. */
public final class MinecraftForgeClient {
    /** Forge's texture bind: inside an original renderer it binds that renderer's texture, otherwise it names the sheet a texture effect draws into. */
    public static void bindTexture(String path){
        // Forge's world block pass switched the tessellator to that texture; a captured block keeps the sheet with its vertices
        if(org.kylepitcock.legacyconnector.vb1_7_3.BlockRenderB173.capturing()){
            legacyconnector.vb1_7_3.net.minecraft.client.render.Tessellator.mcc$sheet(path);return;
        }
        if(org.kylepitcock.legacyconnector.vb1_7_3.RendererContextB173.active()){org.kylepitcock.legacyconnector.vb1_7_3.RendererContextB173.bind(path);return;}
        org.kylepitcock.legacyconnector.vb1_7_3.AnimationsB173.bindSheet(path);
    }
    public static void bindTexture(String path,int sub){bindTexture(path);}
    public static void unbindTexture(){bindTexture("/terrain.png");}
    public static void preloadTexture(String path){}

    private static final java.util.List<IHighlightHandler> HIGHLIGHTS=new java.util.concurrent.CopyOnWriteArrayList<>();
    public static void registerHighlightHandler(IHighlightHandler handler){if(handler!=null)HIGHLIGHTS.add(handler);}
    public static java.util.List<IHighlightHandler> mcc$highlightHandlers(){return HIGHLIGHTS;}

    private static final ThreadLocal<int[]> PASS=ThreadLocal.withInitial(()->new int[1]);
    /** The world render pass being drawn: 0 for opaque blocks, 1 for translucent ones (Forge's multipass blocks). */
    public static int getRenderPass(){return PASS.get()[0];}
    /** Sets the pass {@link #getRenderPass} reports on this thread; returns the previous one. */
    public static int mcc$renderPass(int pass){int[] p=PASS.get();int old=p[0];p[0]=pass;return old;}
}
