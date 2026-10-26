package legacyconnector.v1_7_10.cpw.mods.fml.client;

import legacyconnector.v1_7_10.net.minecraft.client.Minecraft;
import legacyconnector.v1_7_10.net.minecraft.client.entity.EntityClientPlayerMP;
import legacyconnector.v1_7_10.net.minecraft.client.gui.GuiScreen;
import legacyconnector.v1_7_10.net.minecraft.client.multiplayer.WorldClient;

public class FMLClientHandler {
    private static final FMLClientHandler INSTANCE = new FMLClientHandler();

    public static FMLClientHandler instance() {
        return INSTANCE;
    }

    /** Records a texture that failed to load (FML showed them in its error screen); logged instead. */
    public void trackBrokenTexture(legacyconnector.v1_7_10.net.minecraft.util.ResourceLocation location, String error) {
        org.slf4j.LoggerFactory.getLogger("LegacyConnector/Textures").warn("Broken texture {}: {}", location, error);
    }

    public void trackMissingTexture(legacyconnector.v1_7_10.net.minecraft.util.ResourceLocation location) {
        org.slf4j.LoggerFactory.getLogger("LegacyConnector/Textures").warn("Missing texture {}", location);
    }

    /** The resource packs mods contributed (FML kept them here; mods read it by reflection to add their own). */
    private final java.util.List resourcePackList = new java.util.ArrayList();
    private final java.util.Map resourcePackMap = new java.util.HashMap();

    public Minecraft getClient() {
        return Minecraft.getMinecraft();
    }

    public EntityClientPlayerMP getClientPlayerEntity() {
        return Minecraft.mcc$get$thePlayer(getClient());
    }

    public WorldClient getWorldClient() {
        return Minecraft.mcc$get$theWorld(getClient());
    }

    public void showGuiScreen(Object screen) {
        if (screen instanceof GuiScreen s || screen == null) getClient().displayGuiScreen((GuiScreen) screen);
    }

    /** Opens a client-only screen (books, manuals) for the player. */
    public void displayGuiScreen(legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer player, GuiScreen screen) {
        getClient().displayGuiScreen(screen);
    }

    public boolean isGUIOpen(Class<? extends GuiScreen> type) {
        GuiScreen current = Minecraft.mcc$get$currentScreen(getClient());
        return current != null && type.isInstance(current);
    }

    public boolean hasOptifine() {
        return false;
    }

    public legacyconnector.v1_7_10.net.minecraft.network.INetHandler getClientPlayHandler() {
        return legacyconnector.v1_7_10.net.minecraft.client.network.NetHandlerPlayClient.INSTANCE;
    }

    public String getCurrentLanguage() {
        return net.minecraft.client.Minecraft.getInstance().options.languageCode;
    }

    // ------------------------------------------------------------------ resource packs

    /**
     * The pack serving a mod's assets (FML made one per mod at startup; here on first request, over the mod's jar or folder). Mods
     * read other mods' textures through it (Tinkers' texture generators of Extra Utilities and Avaritia).
     */
    @SuppressWarnings("unchecked")
    public legacyconnector.v1_7_10.net.minecraft.client.resources.IResourcePack getResourcePackFor(String modId) {
        Object pack = resourcePackMap.get(modId);
        if (pack == null && modId != null) {
            var container = legacyconnector.v1_7_10.cpw.mods.fml.common.Loader.instance().getIndexedModList().get(modId);
            if (container != null && container.getSource() != null && container.getSource().exists()) {
                pack = mcc$packFor(container);
                if (pack != null) {
                    resourcePackMap.put(modId, pack);
                    resourcePackList.add(pack);
                }
            }
        }
        return (legacyconnector.v1_7_10.net.minecraft.client.resources.IResourcePack) pack;
    }

    /**
     * Adds a mod container's pack to the game's resources (Carpenter's Blocks registers a container over the textures it
     * generates). Packs of loaded legacy mods are already served (translated); others are served as they are by LegacyModPacks.
     */
    @SuppressWarnings("unchecked")
    public void addModAsResource(legacyconnector.v1_7_10.cpw.mods.fml.common.ModContainer container) {
        if (container == null) return;
        Object pack = mcc$packFor(container);
        if (pack == null) return;
        resourcePackList.add(pack);
        resourcePackMap.put(container.getModId(), pack);
        if (!(container instanceof org.kylepitcock.legacyconnector.v1_7_10.LegacyModContainer1710)) {
            org.kylepitcock.legacyconnector.v1_7_10.LegacyModPacks.mcc$addContainer(container);
        }
    }

    private static Object mcc$packFor(legacyconnector.v1_7_10.cpw.mods.fml.common.ModContainer container) {
        try {
            Class<?> cls = container.getCustomResourcePackClass();
            if (cls == null) {
                if (container.getSource() == null) return null;
                cls = container.getSource().isDirectory() ? FMLFolderResourcePack.class : FMLFileResourcePack.class;
            }
            return cls.getConstructor(legacyconnector.v1_7_10.cpw.mods.fml.common.ModContainer.class).newInstance(container);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            org.slf4j.LoggerFactory.getLogger("LegacyConnector/ModPacks").warn("No resource pack for {}: {}", container.getModId(), e.toString());
            return null;
        }
    }

    // ------------------------------------------------------------------ config screens, servers and saves

    /** The mod's {@code @Mod(guiFactory)} instance, or null. */
    public IModGuiFactory getGuiFactoryFor(legacyconnector.v1_7_10.cpw.mods.fml.common.ModContainer container) {
        if (!(container instanceof org.kylepitcock.legacyconnector.v1_7_10.LegacyModContainer1710 legacy)) return null;
        return org.kylepitcock.legacyconnector.v1_7_10.client.LegacyConfigScreens.factory(legacy.record());
    }

    /** Joins a server entry, coming back to the current screen if the connection fails. */
    public void connectToServer(GuiScreen guiMultiplayer, legacyconnector.v1_7_10.net.minecraft.client.multiplayer.ServerData serverEntry) {
        if (serverEntry == null || serverEntry.serverIP == null) return;
        var mc = net.minecraft.client.Minecraft.getInstance();
        var modern = new net.minecraft.client.multiplayer.ServerData(serverEntry.serverName == null ? serverEntry.serverIP : serverEntry.serverName,
                serverEntry.serverIP, net.minecraft.client.multiplayer.ServerData.Type.OTHER);
        net.minecraft.client.gui.screens.Screen parent = mc.screen != null ? mc.screen : new net.minecraft.client.gui.screens.TitleScreen();
        net.minecraft.client.gui.screens.ConnectScreen.startConnecting(parent, mc,
                net.minecraft.client.multiplayer.resolver.ServerAddress.parseString(serverEntry.serverIP), modern, false, null);
    }

    /** FML reset its extended server list data (mod lists of pinged servers) here; the modern game keeps its own. */
    public void setupServerList() {
    }

    public java.io.File getSavesDir() {
        return new java.io.File(getClient().mcDataDir, "saves");
    }

    /** The integrated server (single player and LAN), or null when connected to a remote server. */
    public legacyconnector.v1_7_10.net.minecraft.server.MinecraftServer getServer() {
        var server = net.minecraft.client.Minecraft.getInstance().getSingleplayerServer();
        return server == null ? null : legacyconnector.v1_7_10.net.minecraft.server.MinecraftServer.mcc$wrap(server);
    }

    /** FML drew its own notification screens over the loading screen; there are none, so the game's loading screen draws. */
    public boolean handleLoadingScreen(legacyconnector.v1_7_10.net.minecraft.client.gui.ScaledResolution scaledResolution) {
        return false;
    }

    private net.minecraft.network.Connection mcc$connection;
    private legacyconnector.v1_7_10.net.minecraft.network.NetworkManager mcc$networkManager;

    /** The client's connection to the server (null when not connected), seen as a legacy NetworkManager. */
    public legacyconnector.v1_7_10.net.minecraft.network.NetworkManager getClientToServerNetworkManager() {
        var listener = net.minecraft.client.Minecraft.getInstance().getConnection();
        if (listener == null) return null;
        net.minecraft.network.Connection connection = listener.getConnection();
        if (connection != mcc$connection || mcc$networkManager == null) {
            mcc$connection = connection;
            mcc$networkManager = new legacyconnector.v1_7_10.net.minecraft.network.NetworkManager() {
                @Override
                public boolean isLocalChannel() {
                    return connection.isMemoryConnection();
                }

                @Override
                public boolean isChannelOpen() {
                    return connection.isConnected();
                }

                @Override
                public java.net.SocketAddress getSocketAddress() {
                    return connection.getRemoteAddress();
                }

                @Override
                public io.netty.channel.Channel channel() {
                    return connection.channel();
                }
            };
        }
        return mcc$networkManager;
    }
}
