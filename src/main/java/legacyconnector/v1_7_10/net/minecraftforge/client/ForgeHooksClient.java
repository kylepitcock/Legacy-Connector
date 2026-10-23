package legacyconnector.v1_7_10.net.minecraftforge.client;

/**
 * Forge client hooks. {@code worldRenderPass} is the world pass being meshed (0 cut out, 1 translucent): block renderers
 * that draw in both passes (BuildCraft pipes) read it to decide what to emit. -1 outside chunk meshing.
 */
public class ForgeHooksClient {
    public static int worldRenderPass = -1;

    public static int getWorldRenderPass() {
        return worldRenderPass;
    }
}
