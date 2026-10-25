package legacyconnector.v1_7_10.cpw.mods.fml.server;

import legacyconnector.v1_7_10.net.minecraft.server.MinecraftServer;

/** FML's server side handler: mods reach the running server through it. */
public class FMLServerHandler {
    private static final FMLServerHandler INSTANCE = new FMLServerHandler();

    public static FMLServerHandler instance() {
        return INSTANCE;
    }

    public MinecraftServer getServer() {
        return legacyconnector.v1_7_10.cpw.mods.fml.common.FMLCommonHandler.instance().getMinecraftServerInstance();
    }
}
