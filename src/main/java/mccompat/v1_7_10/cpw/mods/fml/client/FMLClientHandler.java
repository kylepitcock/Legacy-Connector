package mccompat.v1_7_10.cpw.mods.fml.client;

import mccompat.v1_7_10.net.minecraft.client.Minecraft;
import mccompat.v1_7_10.net.minecraft.client.entity.EntityClientPlayerMP;
import mccompat.v1_7_10.net.minecraft.client.gui.GuiScreen;
import mccompat.v1_7_10.net.minecraft.client.multiplayer.WorldClient;

public class FMLClientHandler {
    private static final FMLClientHandler INSTANCE = new FMLClientHandler();

    public static FMLClientHandler instance() {
        return INSTANCE;
    }

    /** Records a texture that failed to load (FML showed them in its error screen); logged instead. */
    public void trackBrokenTexture(mccompat.v1_7_10.net.minecraft.util.ResourceLocation location, String error) {
        org.slf4j.LoggerFactory.getLogger("Retrofit/Textures").warn("Broken texture {}: {}", location, error);
    }

    public void trackMissingTexture(mccompat.v1_7_10.net.minecraft.util.ResourceLocation location) {
        org.slf4j.LoggerFactory.getLogger("Retrofit/Textures").warn("Missing texture {}", location);
    }

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
