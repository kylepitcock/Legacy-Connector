package org.kylepitcock.mccompat.v1_7_10.client;

import java.lang.reflect.Method;
import mccompat.v1_7_10.cpw.mods.fml.common.network.IGuiHandler;
import mccompat.v1_7_10.net.minecraft.client.gui.GuiScreen;
import mccompat.v1_7_10.net.minecraft.client.gui.inventory.GuiContainer;
import mccompat.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import mccompat.v1_7_10.net.minecraft.inventory.Container;
import mccompat.v1_7_10.net.minecraft.world.World;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import org.kylepitcock.mccompat.v1_7_10.bridge.EntityBridge;
import org.kylepitcock.mccompat.v1_7_10.bridge.GuiBridge;
import org.kylepitcock.mccompat.v1_7_10.gui.LegacyMenu;
import org.slf4j.LoggerFactory;

/** Client-side GUI plumbing for 1.7.10 mods. */
public final class ClientGui {
    private ClientGui() {}

    public static void onRegisterScreens(RegisterMenuScreensEvent event) {
        event.register(GuiBridge.menuType(), LegacyContainerScreen::new);
    }

    public static LegacyMenu createMenu(MenuType<LegacyMenu> type, IGuiHandler handler, int windowId, Inventory inv, int id, int x, int y, int z) {
        Object gui = null;
        if (handler != null) {
            EntityPlayer player = (EntityPlayer) EntityBridge.wrap(inv.player);
            try {
                gui = handler.getClientGuiElement(id, player, World.mcc$wrap(inv.player.level()), x, y, z);
            } catch (RuntimeException | LinkageError e) {
                LoggerFactory.getLogger("MCCompat/Gui").error("Legacy client GUI {} failed to construct", id, e);
            }
        }
        if (gui instanceof GuiContainer gc && gc.inventorySlots != null) return new LegacyMenu(type, windowId, inv, gc.inventorySlots, gc);
        LoggerFactory.getLogger("MCCompat/Gui").warn("Legacy server opened GUI {} but the client element is {}", id, gui);
        return new LegacyMenu(type, windowId, inv, new Empty(), null);
    }

    public static void display(GuiScreen screen) {
        Minecraft mc = Minecraft.getInstance();
        if (screen == null) {
            mc.setScreen(null);
            return;
        }
        mc.setScreen(new LegacyScreen(screen));
    }

    public static GuiScreen currentLegacyScreen() {
        var s = Minecraft.getInstance().screen;
        if (s instanceof LegacyScreen ls) return ls.legacy();
        if (s instanceof LegacyContainerScreen cs) return cs.legacy();
        return null;
    }

    public static void showClientOnly(IGuiHandler handler, EntityPlayer player, int id, World world, int x, int y, int z) {
        Object gui = handler.getClientGuiElement(id, player, world, x, y, z);
        if (gui instanceof GuiScreen screen && !(gui instanceof GuiContainer)) display(screen);
    }

    public static void openFromServer(IGuiHandler handler, int id, int x, int y, int z) {
        var p = Minecraft.getInstance().player;
        if (p == null) return;
        Object gui = handler.getClientGuiElement(id, (EntityPlayer) EntityBridge.wrap(p), World.mcc$wrap(p.level()), x, y, z);
        if (gui instanceof GuiScreen screen) display(screen);
    }

    public static void progressBar(int windowId, int id, int value) {
        var p = Minecraft.getInstance().player;
        if (p != null && p.containerMenu instanceof LegacyMenu menu && menu.containerId == windowId) {
            menu.legacy().updateProgressBar(id, value);
        }
    }

    /** True if a class below the shim overrides the named method. */
    public static boolean overrides(Object obj, String name, Class<?>... params) {
        for (Class<?> c = obj.getClass(); c != null && !c.getName().startsWith("mccompat."); c = c.getSuperclass()) {
            try {
                Method m = c.getDeclaredMethod(name, params);
                return m != null;
            } catch (NoSuchMethodException ignored) {
            }
        }
        return false;
    }

    private static final class Empty extends Container {
        @Override
        public boolean canInteractWith(EntityPlayer player) {
            return false;
        }
    }
}
