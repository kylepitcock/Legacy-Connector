package org.kylepitcock.legacyconnector.v1_7_10.client;

import java.lang.reflect.Method;
import legacyconnector.v1_7_10.cpw.mods.fml.common.network.IGuiHandler;
import legacyconnector.v1_7_10.net.minecraft.client.gui.GuiScreen;
import legacyconnector.v1_7_10.net.minecraft.client.gui.inventory.GuiContainer;
import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraft.inventory.Container;
import legacyconnector.v1_7_10.net.minecraft.world.World;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.EntityBridge;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.GuiBridge;
import org.kylepitcock.legacyconnector.v1_7_10.gui.LegacyMenu;
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
                LoggerFactory.getLogger("LegacyConnector/Gui").error("Legacy client GUI {} failed to construct", id, e);
            }
        }
        if (gui instanceof GuiContainer gc && gc.inventorySlots != null) return new LegacyMenu(type, windowId, inv, gc.inventorySlots, gc);
        LoggerFactory.getLogger("LegacyConnector/Gui").warn("Legacy server opened GUI {} but the client element is {}", id, gui);
        return new LegacyMenu(type, windowId, inv, new Empty(), null);
    }

    public static void display(GuiScreen screen) {
        Minecraft mc = Minecraft.getInstance();
        if (screen == null) {
            mc.setScreen(null);
            return;
        }
        net.minecraft.client.gui.screens.Screen vanilla = vanillaEquivalent(mc, screen);
        mc.setScreen(vanilla != null ? vanilla : new LegacyScreen(screen));
    }

    /**
     * 1.7.10's own screens (world list, options, ...) are not implemented as legacy GUIs: a mod that opens one (Custom Main Menu's buttons) gets
     * the modern screen that does the same job, with the screen showing now as its parent.
     */
    private static net.minecraft.client.gui.screens.Screen vanillaEquivalent(Minecraft mc, GuiScreen screen) {
        Class<?> cls = screen.getClass();
        if (!cls.getName().contains("net.minecraft.client.gui.") && !cls.getName().contains("cpw.mods.fml.client.")) return null;
        net.minecraft.client.gui.screens.Screen parent = mc.screen;
        return switch (cls.getSimpleName()) {
            case "GuiMainMenu" -> new net.minecraft.client.gui.screens.TitleScreen();
            case "GuiSelectWorld" -> new net.minecraft.client.gui.screens.worldselection.SelectWorldScreen(parent);
            case "GuiCreateWorld" -> {
                net.minecraft.client.gui.screens.worldselection.CreateWorldScreen.openFresh(mc, parent);
                yield mc.screen;
            }
            case "GuiMultiplayer" -> new net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen(parent);
            case "GuiOptions", "GuiSnooper", "GuiScreenResourcePacks" -> new net.minecraft.client.gui.screens.options.OptionsScreen(parent, mc.options);
            case "GuiLanguage" -> new net.minecraft.client.gui.screens.options.LanguageSelectScreen(parent, mc.options, mc.getLanguageManager());
            case "GuiScreenOptionsSounds" -> new net.minecraft.client.gui.screens.options.SoundOptionsScreen(parent, mc.options);
            case "GuiVideoSettings" -> new net.minecraft.client.gui.screens.options.VideoSettingsScreen(parent, mc, mc.options);
            case "GuiControls" -> new net.minecraft.client.gui.screens.options.controls.ControlsScreen(parent, mc.options);
            case "GuiIngameMenu" -> new net.minecraft.client.gui.screens.PauseScreen(true);
            case "GuiModList" -> new net.neoforged.neoforge.client.gui.ModListScreen(parent);
            default -> null;
        };
    }

    public static GuiScreen currentLegacyScreen() {
        var s = Minecraft.getInstance().screen;
        if (s instanceof LegacyScreen ls) return ls.legacy();
        if (s instanceof LegacyContainerScreen cs) return cs.legacy();
        // Any other open screen (chat, pause, a modern mod's): legacy code only asks whether one is open, e.g. to stop
        // reacting to hotkeys while the player types.
        return s == null ? null : FOREIGN_SCREEN;
    }

    private static final GuiScreen FOREIGN_SCREEN = new GuiScreen();

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
        for (Class<?> c = obj.getClass(); c != null && !c.getName().startsWith("legacyconnector."); c = c.getSuperclass()) {
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
