package org.kylepitcock.legacyconnector.v1_7_10.client;

import legacyconnector.v1_7_10.net.minecraft.client.gui.GuiMainMenu;
import legacyconnector.v1_7_10.net.minecraft.client.gui.GuiScreen;
import legacyconnector.v1_7_10.net.minecraftforge.client.event.GuiOpenEvent;
import legacyconnector.v1_7_10.net.minecraftforge.client.event.GuiScreenEvent;
import legacyconnector.v1_7_10.net.minecraftforge.common.MinecraftForge;
import net.minecraft.client.gui.screens.TitleScreen;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Feeds screen events to the legacy Forge bus: GuiOpenEvent for the title screen (seen as a {@link GuiMainMenu}) and for
 * legacy screens, and the init/draw events of legacy screens. Screens a replacing mod returns are hosted by {@link LegacyScreen}.
 */
public final class ScreenEvents1710 {
    private ScreenEvents1710() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener(ScreenEvents1710::onOpening);
        NeoForge.EVENT_BUS.addListener(ModernViews1710::onInit);
        NeoForge.EVENT_BUS.addListener((ScreenEvent.Closing e) -> ModernViews1710.closed(e.getScreen()));
        NeoForge.EVENT_BUS.addListener((ScreenEvent.MouseScrolled.Pre e) -> legacyconnector.v1_7_10.org.lwjgl.input.Mouse.mcc$scrolled(e.getScrollDeltaY()));
        NeoForge.EVENT_BUS.addListener((ScreenEvent.Init.Post e) -> {
            GuiScreen legacy = hosted(e.getScreen());
            if (legacy == null || !wants(GuiScreenEvent.InitGuiEvent.Post.class)) return;
            GuiScreenEvent.InitGuiEvent.Post post = new GuiScreenEvent.InitGuiEvent.Post(legacy, legacy.mcc$buttons());
            MinecraftForge.EVENT_BUS.post(post);
        });
        NeoForge.EVENT_BUS.addListener((ScreenEvent.Render.Post e) -> {
            if (!wants(GuiScreenEvent.DrawScreenEvent.Post.class)) return;
            // a game screen is seen through its stand-in, as for InitGuiEvent (ModernViews1710), so a mod drawing on every screen (cursor effects) still does
            GuiScreen legacy = hosted(e.getScreen());
            if (legacy == null) legacy = ModernViews1710.of(e.getScreen());
            RenderContext.beginGui(e.getGuiGraphics());
            try {
                MinecraftForge.EVENT_BUS.post(new GuiScreenEvent.DrawScreenEvent.Post(legacy, e.getMouseX(), e.getMouseY(), e.getPartialTick()));
            } finally {
                RenderContext.end();
            }
        });
    }

    /** The legacy screen a hosting modern screen shows: a replaced screen or a legacy container GUI (both are GuiScreens to Forge's events). */
    private static GuiScreen hosted(net.minecraft.client.gui.screens.Screen screen) {
        if (screen instanceof LegacyScreen ls) return ls.legacy();
        if (screen instanceof LegacyContainerScreen lcs) return lcs.legacy();
        return null;
    }

    static boolean wants(Class<? extends legacyconnector.v1_7_10.cpw.mods.fml.common.eventhandler.Event> type) {
        return MinecraftForge.EVENT_BUS.mcc$hasListeners(type);
    }

    private static void onOpening(ScreenEvent.Opening e) {
        if (!wants(GuiOpenEvent.class)) return;
        GuiScreen original;
        if (e.getNewScreen() instanceof TitleScreen) original = new GuiMainMenu();
        else if (e.getNewScreen() instanceof LegacyScreen ls) original = ls.legacy();
        else return;
        GuiOpenEvent legacy = new GuiOpenEvent(original);
        if (MinecraftForge.EVENT_BUS.post(legacy)) {
            e.setCanceled(true);
        } else if (legacy.gui != original) {
            e.setNewScreen(legacy.gui == null ? null : new LegacyScreen(legacy.gui));
        }
    }
}
