package org.kylepitcock.legacyconnector.v1_12_2.client;

import java.util.ArrayList;
import java.util.List;
import legacyconnector.v1_12_2.net.minecraft.client.gui.GuiScreen;
import legacyconnector.v1_12_2.net.minecraft.client.gui.inventory.GuiContainer;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.inventory.Container;
import legacyconnector.v1_12_2.net.minecraft.world.World;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.network.IGuiHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.EntityBridge1122;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.GuiBridge1122;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.WorldBridge1122;
import org.kylepitcock.legacyconnector.v1_12_2.gui.LegacyMenu1122;
import org.slf4j.LoggerFactory;

/** Client-side GUI plumbing for 1.12.2 mods: legacy screens in modern ones, container GUIs on legacy menus. */
public final class ClientGui1122 {
    private ClientGui1122() {}

    public static void onRegisterScreens(net.neoforged.neoforge.client.event.RegisterMenuScreensEvent event) {
        if (GuiBridge1122.menuType() != null) event.register(GuiBridge1122.menuType(), LegacyContainerScreen1122::new);
    }

    public static boolean isLocalPlayer(net.minecraft.world.entity.Entity entity) {
        return entity != null && entity == Minecraft.getInstance().player;
    }

    private static EntityPlayer player(net.minecraft.world.entity.player.Player p) {
        return EntityBridge1122.wrap(p) instanceof EntityPlayer ep ? ep : null;
    }

    public static LegacyMenu1122 createMenu(MenuType<LegacyMenu1122> type, IGuiHandler handler, int windowId, Inventory inv, int id, int x, int y, int z) {
        Object gui = null;
        if (handler != null) {
            try {
                gui = handler.getClientGuiElement(id, player(inv.player), WorldBridge1122.wrap(inv.player.level()), x, y, z);
            } catch (RuntimeException | LinkageError e) {
                LoggerFactory.getLogger("LegacyConnector/Gui").error("Legacy client GUI {} failed to construct", id, e);
            }
        }
        if (gui instanceof GuiContainer gc && gc.inventorySlots != null) return new LegacyMenu1122(type, windowId, inv, gc.inventorySlots, gc);
        LoggerFactory.getLogger("LegacyConnector/Gui").warn("Legacy server opened GUI {} but the client element is {}", id, gui);
        return new LegacyMenu1122(type, windowId, inv, new Empty(), null);
    }

    /** Shows a legacy screen (or closes the open one for null); 1.12's own screens become their modern equivalents. */
    public static void display(GuiScreen screen) {
        Minecraft mc = Minecraft.getInstance();
        if (screen == null) {
            mc.setScreen(null);
            return;
        }
        GuiScreen[] replace = new GuiScreen[1];
        if (ScreenEvents1122.openLegacy(screen, replace)) return;
        mc.setScreen(host(replace[0]));
    }

    /** The modern screen showing a legacy one: the game's own equivalent for 1.12's screens, else a host of the legacy screen. */
    public static net.minecraft.client.gui.screens.Screen host(GuiScreen screen) {
        if (screen == null) return null;
        if (screen instanceof ScreenEvents1122.View view && view.mcc$modern() != null) return view.mcc$modern();
        Minecraft mc = Minecraft.getInstance();
        net.minecraft.client.gui.screens.Screen vanilla = vanillaEquivalent(mc, screen);
        return vanilla != null ? vanilla : new LegacyScreen1122(screen);
    }

    /**
     * 1.12's own screens are not implemented as legacy GUIs: a mod that opens one (a main menu button, the options) gets the modern screen
     * doing the same job, with the open screen as its parent.
     */
    private static net.minecraft.client.gui.screens.Screen vanillaEquivalent(Minecraft mc, GuiScreen screen) {
        String name = screen.getClass().getName();
        if (!name.startsWith("legacyconnector.") && !name.contains("net.minecraft.client.gui.")) return null;
        net.minecraft.client.gui.screens.Screen parent = mc.screen;
        return switch (screen.getClass().getSimpleName()) {
            case "GuiMainMenu" -> new net.minecraft.client.gui.screens.TitleScreen();
            case "GuiWorldSelection" -> new net.minecraft.client.gui.screens.worldselection.SelectWorldScreen(parent);
            case "GuiMultiplayer" -> new net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen(parent);
            case "GuiOptions" -> new net.minecraft.client.gui.screens.options.OptionsScreen(parent, mc.options);
            case "GuiLanguage" -> new net.minecraft.client.gui.screens.options.LanguageSelectScreen(parent, mc.options, mc.getLanguageManager());
            case "GuiScreenOptionsSounds" -> new net.minecraft.client.gui.screens.options.SoundOptionsScreen(parent, mc.options);
            case "GuiVideoSettings" -> new net.minecraft.client.gui.screens.options.VideoSettingsScreen(parent, mc, mc.options);
            case "GuiControls" -> new net.minecraft.client.gui.screens.options.controls.ControlsScreen(parent, mc.options);
            case "GuiIngameMenu" -> new net.minecraft.client.gui.screens.PauseScreen(true);
            case "GuiModList" -> new net.neoforged.neoforge.client.gui.ModListScreen(parent);
            case "GuiInventory" -> mc.player == null ? null : new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player);
            case "GuiChat" -> new net.minecraft.client.gui.screens.ChatScreen(
                    screen instanceof legacyconnector.v1_12_2.net.minecraft.client.gui.GuiChat chat ? chat.mcc$defaultText() : "");
            default -> null;
        };
    }

    /** The legacy screen showing, or a placeholder for any other open screen (legacy code mostly asks whether one is open). */
    public static GuiScreen currentLegacyScreen() {
        var s = Minecraft.getInstance().screen;
        if (s instanceof LegacyScreen1122 ls) return ls.legacy();
        if (s instanceof LegacyContainerScreen1122 cs && cs.legacy() != null) return cs.legacy();
        return ScreenEvents1122.legacyOf(s);
    }

    public static void showClientOnly(IGuiHandler handler, EntityPlayer player, int id, World world, int x, int y, int z) {
        Object gui = handler.getClientGuiElement(id, player, world, x, y, z);
        if (gui instanceof GuiScreen screen && !(gui instanceof GuiContainer)) display(screen);
    }

    public static void openFromServer(IGuiHandler handler, int id, int x, int y, int z) {
        var p = Minecraft.getInstance().player;
        if (p == null) return;
        Object gui = handler.getClientGuiElement(id, player(p), WorldBridge1122.wrap(p.level()), x, y, z);
        if (gui instanceof GuiScreen screen) display(screen);
    }

    /** A stack's tooltip lines with formatting codes, as 1.12's {@code getTooltip} gave them (advanced lines when F3+H is on). */
    public static List<String> tooltipLines(legacyconnector.v1_12_2.net.minecraft.item.ItemStack stack) {
        List<String> out = new ArrayList<>();
        if (stack == null || stack.isEmpty()) return out;
        var mc = Minecraft.getInstance();
        var modern = org.kylepitcock.legacyconnector.v1_12_2.bridge.Stacks1122.toModern(stack);
        var flag = mc.options.advancedItemTooltips ? net.minecraft.world.item.TooltipFlag.Default.ADVANCED : net.minecraft.world.item.TooltipFlag.Default.NORMAL;
        var context = mc.level == null ? net.minecraft.world.item.Item.TooltipContext.EMPTY : net.minecraft.world.item.Item.TooltipContext.of(mc.level);
        for (var line : modern.getTooltipLines(context, mc.player, flag)) out.add(legacyText(line));
        return out;
    }

    /** A component as a string with {@code §} formatting codes. */
    public static String legacyText(net.minecraft.network.chat.Component component) {
        StringBuilder sb = new StringBuilder();
        component.visit((style, text) -> {
            if (text.isEmpty()) return java.util.Optional.empty();
            sb.append('§').append('r');
            if (style.getColor() != null) {
                int rgb = style.getColor().getValue();
                for (var f : net.minecraft.ChatFormatting.values()) {
                    if (f.isColor() && f.getColor() != null && f.getColor() == rgb) {
                        sb.append('§').append(f.getChar());
                        break;
                    }
                }
            }
            if (style.isObfuscated()) sb.append("§k");
            if (style.isBold()) sb.append("§l");
            if (style.isStrikethrough()) sb.append("§m");
            if (style.isUnderlined()) sb.append("§n");
            if (style.isItalic()) sb.append("§o");
            sb.append(text);
            return java.util.Optional.empty();
        }, net.minecraft.network.chat.Style.EMPTY);
        String s = sb.toString();
        return s.startsWith("§r") ? s.substring(2) : s;
    }

    private static final class Empty extends Container {
        @Override
        public boolean canInteractWith(EntityPlayer player) {
            return false;
        }
    }
}
