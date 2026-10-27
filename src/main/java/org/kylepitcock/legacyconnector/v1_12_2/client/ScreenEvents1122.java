package org.kylepitcock.legacyconnector.v1_12_2.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import legacyconnector.v1_12_2.net.minecraft.client.gui.GuiButton;
import legacyconnector.v1_12_2.net.minecraft.client.gui.GuiMainMenu;
import legacyconnector.v1_12_2.net.minecraft.client.gui.GuiScreen;
import legacyconnector.v1_12_2.net.minecraftforge.client.event.GuiOpenEvent;
import legacyconnector.v1_12_2.net.minecraftforge.client.event.GuiScreenEvent;
import legacyconnector.v1_12_2.net.minecraftforge.common.MinecraftForge;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.kylepitcock.legacyconnector.v1_7_10.client.RenderContext;

/**
 * Forge's client events for 1.12.2 mods from the modern ones: screens opening (the title screen is announced as a {@link GuiMainMenu}),
 * set up, drawn and their buttons pressed, also for the game's own screens through a stand-in GuiScreen (buttons mods add to those
 * become modern widgets); input while playing, chat, the field of view and sounds.
 */
public final class ScreenEvents1122 {
    private ScreenEvents1122() {}

    private static final Map<Screen, View> VIEWS = new WeakHashMap<>();

    public static void register() {
        NeoForge.EVENT_BUS.addListener(ScreenEvents1122::onOpening);
        NeoForge.EVENT_BUS.addListener(ScreenEvents1122::onInit);
        NeoForge.EVENT_BUS.addListener(ScreenEvents1122::onRenderPre);
        NeoForge.EVENT_BUS.addListener(ScreenEvents1122::onRenderPost);
        NeoForge.EVENT_BUS.addListener(ScreenEvents1122::onBackground);
        InputEvents1122.register();
    }

    static boolean wants(Class<? extends Event> type) {
        return MinecraftForge.EVENT_BUS.mcc$hasListeners(type);
    }

    static boolean post(Event event) {
        return wants(event.getClass()) && MinecraftForge.EVENT_BUS.post(event);
    }

    private static boolean legacyHost(Screen s) {
        return s instanceof LegacyScreen1122 || s instanceof LegacyContainerScreen1122;
    }

    /** The legacy screen a modern one shows, or the stand-in view of a modern screen. */
    static GuiScreen legacyOf(Screen s) {
        if (s instanceof LegacyScreen1122 ls) return ls.legacy();
        if (s instanceof LegacyContainerScreen1122 cs && cs.legacy() != null) return cs.legacy();
        if (s == null) return null;
        synchronized (VIEWS) {
            return VIEWS.computeIfAbsent(s, View::new);
        }
    }

    /** The GuiOpenEvent of legacy screens is posted when they are displayed ({@code ClientGui1122.display}); this one is for the game's screens. */
    private static void onOpening(ScreenEvent.Opening e) {
        if (legacyHost(e.getNewScreen()) || !wants(GuiOpenEvent.class)) return;
        GuiScreen original = e.getNewScreen() instanceof TitleScreen ? new GuiMainMenu() : legacyOf(e.getNewScreen());
        GuiOpenEvent legacy = new GuiOpenEvent(original);
        if (MinecraftForge.EVENT_BUS.post(legacy)) {
            e.setCanceled(true);
        } else if (legacy.getGui() != original) {
            e.setNewScreen(legacy.getGui() == null ? null : ClientGui1122.host(legacy.getGui()));
        }
    }

    /** A legacy screen being displayed: true if a handler cancelled it; {@code replace[0]} is the screen to show instead. */
    static boolean openLegacy(GuiScreen screen, GuiScreen[] replace) {
        replace[0] = screen;
        if (!wants(GuiOpenEvent.class)) return false;
        GuiOpenEvent event = new GuiOpenEvent(screen);
        if (MinecraftForge.EVENT_BUS.post(event)) return true;
        replace[0] = event.getGui();
        return false;
    }

    private static void onInit(ScreenEvent.Init.Post e) {
        Screen screen = e.getScreen();
        if (legacyHost(screen)) return; // their InitGuiEvents are posted by GuiScreen.setWorldAndResolution
        if (!wants(GuiScreenEvent.InitGuiEvent.class)) return;
        View view = (View) legacyOf(screen);
        view.mcc$resize(screen.width, screen.height);
        List<GuiButton> buttons = view.mcc$buttons();
        buttons.clear();
        post(new GuiScreenEvent.InitGuiEvent.Pre(view, buttons));
        post(new GuiScreenEvent.InitGuiEvent.Post(view, buttons));
        for (GuiButton b : new ArrayList<>(buttons)) e.addListener(new ButtonWidget(view, b));
    }

    private static void onRenderPre(ScreenEvent.Render.Pre e) {
        if (legacyHost(e.getScreen()) || !wants(GuiScreenEvent.DrawScreenEvent.Pre.class)) return;
        RenderContext.beginGui(e.getGuiGraphics());
        try {
            if (post(new GuiScreenEvent.DrawScreenEvent.Pre(legacyOf(e.getScreen()), e.getMouseX(), e.getMouseY(), e.getPartialTick()))) e.setCanceled(true);
        } finally {
            RenderContext.end();
        }
    }

    private static void onRenderPost(ScreenEvent.Render.Post e) {
        if (legacyHost(e.getScreen()) || !wants(GuiScreenEvent.DrawScreenEvent.Post.class)) return;
        RenderContext.beginGui(e.getGuiGraphics());
        try {
            post(new GuiScreenEvent.DrawScreenEvent.Post(legacyOf(e.getScreen()), e.getMouseX(), e.getMouseY(), e.getPartialTick()));
        } finally {
            RenderContext.end();
        }
    }

    private static void onBackground(ScreenEvent.BackgroundRendered e) {
        if (legacyHost(e.getScreen()) || !wants(GuiScreenEvent.BackgroundDrawnEvent.class)) return;
        RenderContext.beginGui(e.getGuiGraphics());
        try {
            post(new GuiScreenEvent.BackgroundDrawnEvent(legacyOf(e.getScreen())));
        } finally {
            RenderContext.end();
        }
    }

    /** Draw-screen events around a hosted legacy screen's drawing; returns false when a handler cancelled the drawing. */
    static boolean drawPre(GuiScreen legacy, int mouseX, int mouseY, float partialTicks) {
        return !post(new GuiScreenEvent.DrawScreenEvent.Pre(legacy, mouseX, mouseY, partialTicks));
    }

    static void drawPost(GuiScreen legacy, int mouseX, int mouseY, float partialTicks) {
        post(new GuiScreenEvent.DrawScreenEvent.Post(legacy, mouseX, mouseY, partialTicks));
    }

    /** A modern screen as a GuiScreen: its size, the buttons mods add to it. */
    public static final class View extends GuiScreen {
        private final java.lang.ref.WeakReference<Screen> modern;

        View(Screen modern) {
            this.modern = new java.lang.ref.WeakReference<>(modern);
            this.mc = legacyconnector.v1_12_2.net.minecraft.client.Minecraft.getMinecraft();
            this.fontRenderer = mc.fontRenderer;
        }

        public Screen mcc$modern() {
            return modern.get();
        }

        void mcc$resize(int w, int h) {
            setGuiSize(w, h);
        }

        @Override
        public boolean doesGuiPauseGame() {
            Screen s = modern.get();
            return s != null && s.isPauseScreen();
        }
    }

    /** A legacy button added to a modern screen: drawn by the legacy code, its press announced as ActionPerformedEvent. */
    private static final class ButtonWidget extends AbstractWidget {
        private final View view;
        private final GuiButton button;

        ButtonWidget(View view, GuiButton button) {
            super(button.x, button.y, button.width, button.height, Component.literal(button.displayString == null ? "" : button.displayString));
            this.view = view;
            this.button = button;
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            setX(button.x);
            setY(button.y);
            setWidth(button.width);
            setHeight(button.height);
            visible = button.visible;
            RenderContext.beginGui(graphics);
            try {
                button.drawButton(legacyconnector.v1_12_2.net.minecraft.client.Minecraft.getMinecraft(), mouseX, mouseY, partialTick);
            } catch (RuntimeException | LinkageError e) {
                button.visible = false;
                org.slf4j.LoggerFactory.getLogger("LegacyConnector/Gui").warn("A legacy button {} failed to draw: {}", button.getClass().getName(), e.toString());
            } finally {
                RenderContext.end();
            }
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
            if (mouseButton != 0 || !button.mousePressed(legacyconnector.v1_12_2.net.minecraft.client.Minecraft.getMinecraft(), (int) mouseX, (int) mouseY)) return false;
            var pre = new GuiScreenEvent.ActionPerformedEvent.Pre(view, button, view.mcc$buttons());
            if (post(pre)) return true;
            pre.getButton().playPressSound(legacyconnector.v1_12_2.net.minecraft.client.audio.SoundHandler.mcc$INSTANCE);
            post(new GuiScreenEvent.ActionPerformedEvent.Post(view, pre.getButton(), view.mcc$buttons()));
            return true;
        }

        @Override
        public void onRelease(double mouseX, double mouseY) {
            button.mouseReleased((int) mouseX, (int) mouseY);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }
    }
}
