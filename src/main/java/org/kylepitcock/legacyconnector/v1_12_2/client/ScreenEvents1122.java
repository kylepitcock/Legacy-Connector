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

    private static final Map<Screen, GuiScreen> VIEWS = new WeakHashMap<>();

    /** A legacy stand-in of a modern screen. */
    public interface Modern {
        Screen mcc$modern();

        /** Copies the modern screen's size and state into the stand-in. */
        void mcc$sync();
    }

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
        GuiScreen view;
        synchronized (VIEWS) {
            view = VIEWS.computeIfAbsent(s, ModernScreens1122::viewOf);
        }
        if (view instanceof Modern m) m.mcc$sync();
        return view;
    }

    /** The GuiOpenEvent of legacy screens is posted when they are displayed ({@code ClientGui1122.display}); this one is for the game's screens. */
    private static void onOpening(ScreenEvent.Opening e) {
        if (legacyHost(e.getNewScreen()) || !wants(GuiOpenEvent.class)) return;
        GuiScreen original = legacyOf(e.getNewScreen());
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
        // a listener of Pre or Post does not count as one of the base class
        if (!wants(GuiScreenEvent.InitGuiEvent.Pre.class) && !wants(GuiScreenEvent.InitGuiEvent.Post.class)) return;
        GuiScreen view = legacyOf(screen);
        view.setGuiSize(screen.width, screen.height);
        if (view instanceof Modern m) m.mcc$sync();
        List<GuiButton> buttons = view.mcc$buttons();
        buttons.clear();
        // the screen's own buttons, as GuiButtons, so handlers can find "Mods", place theirs beside it, move or remove vanilla ones
        List<Mirror> mirrors = new ArrayList<>();
        int index = 0;
        for (var child : new ArrayList<>(screen.children())) {
            if (child instanceof net.minecraft.client.gui.components.AbstractButton w) {
                Mirror m = new Mirror(w, mirrorId(w, index++));
                mirrors.add(m);
                buttons.add(m);
            }
        }
        post(new GuiScreenEvent.InitGuiEvent.Pre(view, buttons));
        post(new GuiScreenEvent.InitGuiEvent.Post(view, buttons));
        List<int[]> taken = new ArrayList<>();
        for (Mirror m : mirrors) {
            m.syncBack(buttons.contains(m));
            if (buttons.contains(m) && m.visible) taken.add(new int[] {m.x, m.y, m.width, m.height});
        }
        for (GuiButton b : new ArrayList<>(buttons)) {
            if (b instanceof Mirror) continue;
            if (b.visible) {
                // mods placed their buttons for the old layout: one on top of a vanilla button moves to the nearest free spot
                int[] spot = org.kylepitcock.legacyconnector.client.ButtonPlacement.freeSpot(new int[] {b.x, b.y, b.width, b.height}, taken, screen.width, screen.height);
                b.x = spot[0];
                b.y = spot[1];
                taken.add(spot);
            }
            e.addListener(new ButtonWidget(view, b));
        }
    }

    /** 1.12.2's button ids for the title and pause screens' buttons that mods look for; others are numbered from 1000. */
    private static int mirrorId(net.minecraft.client.gui.components.AbstractButton w, int index) {
        if (w.getMessage().getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents t) {
            switch (t.getKey()) {
                case "menu.singleplayer" -> {
                    return 1;
                }
                case "menu.multiplayer" -> {
                    return 2;
                }
                case "menu.online" -> {
                    return 14;
                }
                case "menu.options" -> {
                    return 0;
                }
                case "menu.quit" -> {
                    return 4;
                }
                case "menu.returnToMenu", "menu.disconnect" -> {
                    return 1;
                }
                case "menu.returnToGame" -> {
                    return 4;
                }
                case "gui.advancements" -> {
                    return 5;
                }
                case "gui.stats" -> {
                    return 6;
                }
                case "menu.shareToLan" -> {
                    return 7;
                }
                default -> { }
            }
        }
        return 1000 + index;
    }

    /** One of the screen's own buttons, as a GuiButton the handlers can read, move, rename, hide or remove. */
    private static final class Mirror extends GuiButton {
        private final net.minecraft.client.gui.components.AbstractButton widget;
        private final int x0;
        private final int y0;
        private final String text0;

        Mirror(net.minecraft.client.gui.components.AbstractButton widget, int id) {
            super(id, widget.getX(), widget.getY(), widget.getWidth(), widget.getHeight(), widget.getMessage().getString());
            this.widget = widget;
            this.x0 = x;
            this.y0 = y;
            this.text0 = displayString;
            this.enabled = widget.active;
            this.visible = widget.visible;
        }

        void syncBack(boolean kept) {
            if (!kept) {
                widget.visible = false;
                widget.active = false;
                return;
            }
            if (x != x0 || y != y0) widget.setPosition(x, y);
            if (width != widget.getWidth()) widget.setWidth(width);
            if (height != widget.getHeight()) widget.setHeight(height);
            widget.visible = visible;
            widget.active = enabled;
            if (displayString != null && !displayString.equals(text0)) widget.setMessage(Component.literal(displayString));
        }
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
    public static final class View extends GuiScreen implements Modern {
        private final java.lang.ref.WeakReference<Screen> modern;

        View(Screen modern) {
            this.modern = new java.lang.ref.WeakReference<>(modern);
            mcc$initView(legacyconnector.v1_12_2.net.minecraft.client.Minecraft.getMinecraft(), modern.width, modern.height);
        }

        @Override
        public Screen mcc$modern() {
            return modern.get();
        }

        @Override
        public void mcc$sync() {
            Screen s = modern.get();
            if (s != null) setGuiSize(s.width, s.height);
        }

        @Override
        public boolean doesGuiPauseGame() {
            Screen s = modern.get();
            return s != null && s.isPauseScreen();
        }
    }

    /** A legacy button added to a modern screen: drawn by the legacy code, its press announced as ActionPerformedEvent. */
    private static final class ButtonWidget extends AbstractWidget {
        private final GuiScreen view;
        private final GuiButton button;

        ButtonWidget(GuiScreen view, GuiButton button) {
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
