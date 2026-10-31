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
        if (screen instanceof LegacyConfigScreens.ModernParent parent) {
            mc.setScreen(parent.modern());
            return;
        }
        net.minecraft.client.gui.screens.Screen vanilla = vanillaEquivalent(mc, screen);
        if (vanilla == null && screen instanceof GuiContainer gc && gc.inventorySlots != null && mc.player != null) {
            // a container GUI the client opens by itself (OpenBlocks' guide book on a dummy container): the container screen
            // draws its background layer, components and labels; a plain screen only ran drawScreen, which for a
            // GuiContainer draws just the buttons. The container keeps its own window id, as in 1.7.10.
            var inv = mc.player.getInventory();
            var menu = new LegacyMenu(GuiBridge.menuType(), gc.inventorySlots.windowId, inv, gc.inventorySlots, gc);
            menu.mcc$setDetached(true);
            mc.setScreen(new LegacyContainerScreen(menu, inv, net.minecraft.network.chat.Component.empty()));
            return;
        }
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
            case "ScreenChatOptions" -> new net.minecraft.client.gui.screens.options.ChatOptionsScreen(parent, mc.options);
            case "GuiInventory" -> mc.player == null ? null : new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player);
            case "GuiStats" -> mc.player == null ? null : new net.minecraft.client.gui.screens.achievement.StatsScreen(parent, mc.player.getStats());
            case "GuiChat" -> screen instanceof legacyconnector.v1_7_10.net.minecraft.client.gui.GuiChat chat
                    ? new net.minecraft.client.gui.screens.ChatScreen(chat.mcc$defaultText()) : null;
            case "GuiScreenAddServer" -> screen instanceof legacyconnector.v1_7_10.net.minecraft.client.gui.GuiScreenAddServer add
                    ? add.mcc$modern(parent) : null;
            case "GuiStreamOptions" -> new LegacyScreen(new legacyconnector.v1_7_10.net.minecraft.client.gui.stream.GuiStreamUnavailable(screen));
            default -> null;
        };
    }

    /** The container of a container screen the client opened by itself and no server menu has claimed, or null. */
    public static Container detachedContainer(net.minecraft.world.entity.player.Player player) {
        Minecraft mc = Minecraft.getInstance();
        if (player != mc.player || !(mc.screen instanceof LegacyContainerScreen cs)) return null;
        return cs.getMenu().mcc$detached() ? cs.getMenu().legacy() : null;
    }

    /**
     * The server opened a container by hand (a mod's own packet made this client build the screen, see
     * {@code GuiBridge.openManual}) under this window id: the screen's menu becomes the player's open menu with that id, so the
     * server's slot data, progress bars, clicks and closing find it.
     */
    public static void manualOpen(int windowId) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.screen instanceof LegacyContainerScreen cs && cs.getMenu().mcc$detached() && cs.getMenu().mcc$setWindowId(windowId)) {
            cs.getMenu().mcc$setDetached(false);
            mc.player.containerMenu = cs.getMenu();
            return;
        }
        // no screen to take the menu (the mod's packet failed): tell the server so that its menu does not swallow the inventory clicks
        if (mc.getConnection() != null) mc.getConnection().send(new net.minecraft.network.protocol.game.ServerboundContainerClosePacket(windowId));
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

    /** The last GUI the client opened by itself (handler, id, x, y, z) and when: see {@link #openFromServer}. */
    private static Object[] lastClientOnly;
    private static long lastClientOnlyAt;

    public static void showClientOnly(IGuiHandler handler, EntityPlayer player, int id, World world, int x, int y, int z) {
        Object gui = handler.getClientGuiElement(id, player, world, x, y, z);
        if (gui instanceof GuiScreen screen && !(gui instanceof GuiContainer)) {
            lastClientOnly = new Object[] {handler, id, x, y, z};
            lastClientOnlyAt = System.currentTimeMillis();
            display(screen);
        }
    }

    public static void openFromServer(IGuiHandler handler, int id, int x, int y, int z) {
        var p = Minecraft.getInstance().player;
        if (p == null) return;
        // FML's server did nothing for a GUI without a server element; the client had opened it from its own right click already.
        // Opening it again made a second screen (Witchery's books: the first one closed and sent its page state, the second
        // replaced it) and, for a handler that reads state the first one changed, a different book.
        Object[] last = lastClientOnly;
        if (last != null && System.currentTimeMillis() - lastClientOnlyAt < 2000 && last[0] == handler && last[1].equals(id) && last[2].equals(x)
                && last[3].equals(y) && last[4].equals(z) && Minecraft.getInstance().screen != null) {
            lastClientOnly = null;
            return;
        }
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
