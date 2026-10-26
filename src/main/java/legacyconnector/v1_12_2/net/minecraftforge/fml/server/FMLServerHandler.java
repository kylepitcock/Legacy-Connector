package legacyconnector.v1_12_2.net.minecraftforge.fml.server;

/** FML's dedicated-server side handler; mods reach the running server through it. */
public class FMLServerHandler {
    private static final FMLServerHandler INSTANCE = new FMLServerHandler();

    public static FMLServerHandler instance() {
        return INSTANCE;
    }

    public legacyconnector.v1_12_2.net.minecraft.server.MinecraftServer getServer() {
        return legacyconnector.v1_12_2.net.minecraft.server.MinecraftServer.mcc$instance();
    }

    public legacyconnector.v1_12_2.net.minecraftforge.fml.relauncher.Side getSide() {
        return legacyconnector.v1_12_2.net.minecraftforge.fml.relauncher.Side.SERVER;
    }

    public String getCurrentLanguage() {
        return "en_US";
    }
}
