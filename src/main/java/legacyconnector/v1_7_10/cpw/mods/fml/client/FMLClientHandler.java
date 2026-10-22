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

    public boolean isGUIOpen(Class<? extends GuiScreen> type) {
        GuiScreen current = Minecraft.mcc$get$currentScreen(getClient());
        return current != null && type.isInstance(current);
    }
}
