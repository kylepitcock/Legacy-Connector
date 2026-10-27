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
}
