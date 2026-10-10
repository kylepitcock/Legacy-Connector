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
