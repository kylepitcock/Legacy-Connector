package legacyconnector.v1_12_2.net.minecraftforge.fml.client;

import legacyconnector.v1_12_2.net.minecraft.client.Minecraft;
import legacyconnector.v1_12_2.net.minecraft.client.entity.EntityPlayerSP;
import legacyconnector.v1_12_2.net.minecraft.client.gui.GuiScreen;
import legacyconnector.v1_12_2.net.minecraft.client.multiplayer.WorldClient;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;

/** FML's client side: the client, its player and world, screens, and texture error tracking. */
public class FMLClientHandler {
    private static final FMLClientHandler INSTANCE = new FMLClientHandler();

    public static FMLClientHandler instance() {
        return INSTANCE;
    }

    public Minecraft getClient() {
        return Minecraft.getMinecraft();
    }

    public EntityPlayerSP getClientPlayerEntity() {
        return Minecraft.mcc$get$player(getClient());
    }

    public WorldClient getWorldClient() {
        return Minecraft.mcc$get$world(getClient());
    }

    /** OptiFine does not exist for this game; mods use this to switch off their workarounds. */
    public boolean hasOptifine() {
        return false;
    }

    /** Shows the screen when the player is the client's own player (FML ignored other players). */
    public void displayGuiScreen(EntityPlayer player, GuiScreen gui) {
        if (player == null || player == getClientPlayerEntity()) getClient().displayGuiScreen(gui);
    }

    public void showGuiScreen(Object clientGuiElement) {
        if (clientGuiElement instanceof GuiScreen gui) getClient().displayGuiScreen(gui);
    }

    public GuiScreen getCurrentScreen() {
        return Minecraft.mcc$get$currentScreen(getClient());
    }

    public boolean isGUIOpen(Class<? extends GuiScreen> gui) {
        GuiScreen current = getCurrentScreen();
        return current != null && gui != null && current.getClass() == gui;
    }

    public void trackMissingTexture(ResourceLocation resourceLocation) {
        org.slf4j.LoggerFactory.getLogger("LegacyConnector/Textures").debug("Legacy mod texture missing: {}", resourceLocation);
    }

    public void trackBrokenTexture(ResourceLocation resourceLocation, String error) {
        org.slf4j.LoggerFactory.getLogger("LegacyConnector/Textures").debug("Legacy mod texture broken: {} ({})", resourceLocation, error);
    }

    public boolean isLoading() {
        return net.minecraft.client.Minecraft.getInstance().getOverlay() != null;
    }

    public void refreshResources() {
        net.minecraft.client.Minecraft.getInstance().reloadResourcePacks();
    }

    public legacyconnector.v1_12_2.net.minecraftforge.fml.relauncher.Side getSide() {
        return legacyconnector.v1_12_2.net.minecraftforge.fml.relauncher.Side.CLIENT;
    }

    public legacyconnector.v1_12_2.net.minecraftforge.fml.relauncher.Side getEffectiveSide() {
        return net.minecraft.client.Minecraft.getInstance().isSameThread() ? legacyconnector.v1_12_2.net.minecraftforge.fml.relauncher.Side.CLIENT
                : legacyconnector.v1_12_2.net.minecraftforge.fml.relauncher.Side.SERVER;
    }

    public java.io.File getSavesDir() {
        return net.neoforged.fml.loading.FMLPaths.GAMEDIR.get().resolve("saves").toFile();
    }

    public String getCurrentLanguage() {
        return net.minecraft.client.Minecraft.getInstance().options.languageCode;
    }
    /** The config screen factory of a mod (its {@code @Mod(guiFactory)}, or Forge's default screen for {@code @Config} mods). */
    public IModGuiFactory getGuiFactoryFor(legacyconnector.v1_12_2.net.minecraftforge.fml.common.ModContainer selectedMod) {
        return selectedMod == null ? null : org.kylepitcock.legacyconnector.v1_12_2.client.ConfigScreens1122.factoryFor(selectedMod.getModId());
    }

    /** Reloads the resource packs; the modern game reloads everything (Forge's selective reload has no counterpart). */
    public void refreshResources(legacyconnector.v1_12_2.net.minecraftforge.client.resource.IResourceType... inclusion) {
        refreshResources();
    }

    public legacyconnector.v1_12_2.net.minecraft.network.NetworkManager getClientToServerNetworkManager() {
        var connection = net.minecraft.client.Minecraft.getInstance().getConnection();
        return connection == null ? null : legacyconnector.v1_12_2.net.minecraft.network.NetworkManager.mcc$of(connection.getConnection());
    }

    /** The integrated server while playing single player (null on a remote server). */
    public legacyconnector.v1_12_2.net.minecraft.server.MinecraftServer getServer() {
        return net.minecraft.client.Minecraft.getInstance().hasSingleplayerServer() ? legacyconnector.v1_12_2.net.minecraft.server.MinecraftServer.mcc$instance() : null;
    }

    /** FML drew its own loading or error screen here; the modern game has its own loading overlay, so nothing is drawn. */
    public boolean handleLoadingScreen(legacyconnector.v1_12_2.net.minecraft.client.gui.ScaledResolution scaledResolution) {
        return false;
    }

    /** Rebuilds the world's chunk meshes (after a mod changed how its blocks render). */
    public void reloadRenderers() {
        var mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.levelRenderer != null) mc.levelRenderer.allChanged();
    }
}
