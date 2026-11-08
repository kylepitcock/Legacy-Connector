package org.kylepitcock.legacyconnector.v1_7_10.client;

import java.util.ArrayList;
import java.util.List;
import legacyconnector.v1_7_10.net.minecraft.client.Minecraft;
import legacyconnector.v1_7_10.net.minecraft.client.gui.GuiButton;
import legacyconnector.v1_7_10.net.minecraft.client.gui.GuiControls;
import legacyconnector.v1_7_10.net.minecraft.client.gui.GuiIngameMenu;
import legacyconnector.v1_7_10.net.minecraft.client.gui.GuiMainMenu;
import legacyconnector.v1_7_10.net.minecraft.client.gui.GuiOptions;
import legacyconnector.v1_7_10.net.minecraft.client.gui.GuiScreen;
import legacyconnector.v1_7_10.net.minecraft.client.gui.GuiScreenOptionsSounds;
import legacyconnector.v1_7_10.net.minecraft.client.gui.GuiVideoSettings;
import legacyconnector.v1_7_10.net.minecraftforge.client.event.GuiScreenEvent;
import legacyconnector.v1_7_10.net.minecraftforge.common.MinecraftForge;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.event.ScreenEvent;
import org.slf4j.LoggerFactory;

/**
 * The game's own screens (Options, Video Settings, Controls, Sounds, Pause, Main Menu...) as the 1.7.10 screen each one replaces, so
 * legacy mods' {@code GuiScreenEvent.InitGuiEvent} handlers find them ({@code event.gui instanceof GuiOptions}), add {@code GuiButton}s
 * to {@code buttonList} and handle {@code ActionPerformedEvent}. The buttons become modern widgets of the real screen.
 */
public final class ModernViews1710 {
    private ModernViews1710() {}

    /** A legacy stand-in of a modern screen; displaying it (a mod's sub screen going back to its parent) shows the modern screen. */
    public interface View {
        Screen modern();
    }

    public static final class Plain extends LegacyConfigScreens.ModernParent implements View {
        public Plain(Screen modern) {
            super(modern);
        }
    }

    public static final class MainMenu extends GuiMainMenu implements View {
        private final Screen modern;

        MainMenu(Screen modern) {
            this.modern = modern;
        }

        @Override
        public Screen modern() {
            return modern;
        }
    }

    public static final class Options extends GuiOptions implements View {
        private final Screen modern;

        Options(Screen modern) {
            super(null, Minecraft.mcc$get$gameSettings(Minecraft.getMinecraft()));
            this.modern = modern;
        }

        @Override
        public Screen modern() {
            return modern;
        }
    }

    public static final class Pause extends GuiIngameMenu implements View {
        private final Screen modern;

        Pause(Screen modern) {
            this.modern = modern;
        }

        @Override
        public Screen modern() {
            return modern;
        }
    }

    public static final class Video extends GuiVideoSettings implements View {
        private final Screen modern;

        Video(Screen modern) {
            super(null, Minecraft.mcc$get$gameSettings(Minecraft.getMinecraft()));
            this.modern = modern;
        }

        @Override
        public Screen modern() {
            return modern;
        }
    }

    public static final class Controls extends GuiControls implements View {
        private final Screen modern;

        Controls(Screen modern) {
            super(null, Minecraft.mcc$get$gameSettings(Minecraft.getMinecraft()));
            this.modern = modern;
        }

        @Override
        public Screen modern() {
            return modern;
        }
    }

    public static final class Sounds extends GuiScreenOptionsSounds implements View {
        private final Screen modern;

        Sounds(Screen modern) {
            super(null, Minecraft.mcc$get$gameSettings(Minecraft.getMinecraft()));
            this.modern = modern;
        }

        @Override
        public Screen modern() {
            return modern;
        }
    }

    private static Screen slotScreen;
    private static GuiScreen slotView;

    /** The stand-in of the screen open now (the same object while it stays open, so a mod's sub screen can return to it). */
    static synchronized GuiScreen of(Screen s) {
        if (slotScreen != s || slotView == null) {
            slotScreen = s;
            slotView = viewOf(s);
        }
        slotView.mcc$initView(Minecraft.getMinecraft(), s.width, s.height);
        return slotView;
    }

    static synchronized void closed(Screen s) {
        if (slotScreen == s) {
            slotScreen = null;
            slotView = null;
        }
    }

    private static GuiScreen viewOf(Screen s) {
        try {
            if (s instanceof net.minecraft.client.gui.screens.TitleScreen) return new MainMenu(s);
            if (s instanceof net.minecraft.client.gui.screens.options.OptionsScreen) return new Options(s);
            if (s instanceof net.minecraft.client.gui.screens.PauseScreen) return new Pause(s);
            if (s instanceof net.minecraft.client.gui.screens.options.VideoSettingsScreen) return new Video(s);
            if (s instanceof net.minecraft.client.gui.screens.options.controls.ControlsScreen) return new Controls(s);
            if (s instanceof net.minecraft.client.gui.screens.options.SoundOptionsScreen) return new Sounds(s);
        } catch (RuntimeException | LinkageError e) {
            LoggerFactory.getLogger("LegacyConnector/Gui").debug("No typed legacy view of {}: {}", s.getClass().getName(), e.toString());
        }
        return new Plain(s);
    }

    /**
     * ScreenEvent.Init.Post of a game screen: posts the legacy InitGuiEvents with a stand-in, and the buttons the handlers added
     * become widgets of the screen. Legacy-hosted screens post their own events (ScreenEvents1710).
     */
    static void onInit(ScreenEvent.Init.Post e) {
        Screen screen = e.getScreen();
        if (screen instanceof LegacyScreen || screen instanceof LegacyContainerScreen) return;
        if (!ScreenEvents1710.wants(GuiScreenEvent.InitGuiEvent.Post.class) && !ScreenEvents1710.wants(GuiScreenEvent.InitGuiEvent.Pre.class)) return;
        GuiScreen view = of(screen);
        var buttons = view.mcc$buttons();
        buttons.clear();
        // the screen's own buttons, as GuiButtons, so handlers can find "Mods", place theirs beside it, move or remove vanilla ones
        List<Mirror> mirrors = new ArrayList<>();
        int index = 0;
        for (GuiEventListener child : new ArrayList<>(screen.children())) {
            if (child instanceof AbstractButton w) {
                Mirror m = new Mirror(w, idOf(w, index++));
                mirrors.add(m);
                buttons.add(m);
            }
        }
        try {
            MinecraftForge.EVENT_BUS.post(new GuiScreenEvent.InitGuiEvent.Pre(view, buttons));
            MinecraftForge.EVENT_BUS.post(new GuiScreenEvent.InitGuiEvent.Post(view, buttons));
        } catch (RuntimeException | LinkageError ex) {
            LoggerFactory.getLogger("LegacyConnector/Gui").warn("A legacy InitGuiEvent handler failed on {}: {}", screen.getClass().getSimpleName(), ex.toString());
        }
        List<int[]> taken = new ArrayList<>();
        for (Mirror m : mirrors) {
            m.syncBack(buttons.contains(m));
            if (buttons.contains(m) && m.visible) taken.add(new int[] {m.xPosition, m.yPosition, m.width, m.height});
        }
        for (Object o : new ArrayList<>(buttons)) {
            if (!(o instanceof GuiButton b) || b instanceof Mirror) continue;
            if (b.visible) {
                // mods placed their buttons for the old layout: one on top of a vanilla button moves to the nearest free spot
                int[] spot = org.kylepitcock.legacyconnector.client.ButtonPlacement.freeSpot(new int[] {b.xPosition, b.yPosition, b.width, b.height}, taken, screen.width, screen.height);
                b.xPosition = spot[0];
                b.yPosition = spot[1];
                taken.add(spot);
            }
            e.addListener(new ButtonWidget(view, b));
        }
    }

    /** 1.7.10's button ids for the buttons of the title and pause screens that mods look for; others are numbered from 1000. */
    private static int idOf(AbstractButton w, int index) {
        if (w.getMessage().getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents t) {
            switch (t.getKey()) {
                case "menu.singleplayer", "menu.returnToGame" -> {
                    return t.getKey().equals("menu.singleplayer") ? 1 : 4;
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
                case "menu.quit", "menu.returnToMenu", "menu.disconnect" -> {
                    return 1;
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
        private final AbstractButton widget;
        private final int x0;
        private final int y0;
        private final String text0;

        Mirror(AbstractButton widget, int id) {
            super(id, widget.getX(), widget.getY(), widget.getWidth(), widget.getHeight(), widget.getMessage().getString());
            this.widget = widget;
            this.x0 = xPosition;
            this.y0 = yPosition;
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
            if (xPosition != x0 || yPosition != y0) widget.setPosition(xPosition, yPosition);
            if (width != widget.getWidth()) widget.setWidth(width);
            if (height != widget.getHeight()) widget.setHeight(height);
            widget.visible = visible;
            widget.active = enabled;
            if (displayString != null && !displayString.equals(text0)) widget.setMessage(Component.literal(displayString));
        }
    }

    /** A legacy button added to a game screen: drawn by the legacy code, its press announced as ActionPerformedEvent. */
    private static final class ButtonWidget extends AbstractWidget {
        private final GuiScreen view;
        private final GuiButton button;

        ButtonWidget(GuiScreen view, GuiButton button) {
            super(button.xPosition, button.yPosition, button.width, button.height, Component.literal(button.displayString == null ? "" : button.displayString));
            this.view = view;
            this.button = button;
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            setX(button.xPosition);
            setY(button.yPosition);
            setWidth(button.width);
            setHeight(button.height);
            visible = button.visible;
            RenderContext.beginGui(graphics);
            try {
                button.drawButton(Minecraft.getMinecraft(), mouseX, mouseY);
            } catch (RuntimeException | LinkageError ex) {
                button.visible = false;
                LoggerFactory.getLogger("LegacyConnector/Gui").warn("A legacy button {} failed to draw: {}", button.getClass().getName(), ex.toString());
            } finally {
                RenderContext.end();
            }
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
            if (mouseButton != 0 || !button.mousePressed(Minecraft.getMinecraft(), (int) mouseX, (int) mouseY)) return false;
            press();
            return true;
        }

        @Override
        public boolean keyPressed(int key, int scanCode, int modifiers) {
            if (isFocused() && button.enabled && (key == 257 || key == 32 || key == 335)) {
                press();
                return true;
            }
            return false;
        }

        private void press() {
            var pre = new GuiScreenEvent.ActionPerformedEvent.Pre(view, button, view.mcc$buttons());
            try {
                if (MinecraftForge.EVENT_BUS.post(pre)) return;
                pre.button.playPressSound(legacyconnector.v1_7_10.net.minecraft.client.audio.SoundHandler.INSTANCE);
                MinecraftForge.EVENT_BUS.post(new GuiScreenEvent.ActionPerformedEvent.Post(view, pre.button, view.mcc$buttons()));
            } catch (RuntimeException | LinkageError ex) {
                LoggerFactory.getLogger("LegacyConnector/Gui").warn("A legacy button action failed: {}", ex.toString());
            }
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
