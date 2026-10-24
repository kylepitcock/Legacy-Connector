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
        NeoForge.EVENT_BUS.addListener((ScreenEvent.MouseScrolled.Pre e) -> legacyconnector.v1_7_10.org.lwjgl.input.Mouse.mcc$scrolled(e.getScrollDeltaY()));
        NeoForge.EVENT_BUS.addListener((ScreenEvent.Init.Post e) -> {
            if (!(e.getScreen() instanceof LegacyScreen ls) || !wants(GuiScreenEvent.InitGuiEvent.Post.class)) return;
            GuiScreenEvent.InitGuiEvent.Post post = new GuiScreenEvent.InitGuiEvent.Post(ls.legacy(), ls.legacy().mcc$buttons());
            MinecraftForge.EVENT_BUS.post(post);
        });
        NeoForge.EVENT_BUS.addListener((ScreenEvent.Render.Post e) -> {
            if (!(e.getScreen() instanceof LegacyScreen ls) || !wants(GuiScreenEvent.DrawScreenEvent.Post.class)) return;
            RenderContext.beginGui(e.getGuiGraphics());
            try {
                MinecraftForge.EVENT_BUS.post(new GuiScreenEvent.DrawScreenEvent.Post(ls.legacy(), e.getMouseX(), e.getMouseY(), e.getPartialTick()));
            } finally {
                RenderContext.end();
            }
        });
    }

    private static boolean wants(Class<? extends legacyconnector.v1_7_10.cpw.mods.fml.common.eventhandler.Event> type) {
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
