package org.kylepitcock.legacyconnector.vb1_7_3.client;

import legacyconnector.vb1_7_3.net.minecraft.client.gui.screen.Screen;
import legacyconnector.vb1_7_3.net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import org.kylepitcock.legacyconnector.vb1_7_3.GuiDrawB173;
import org.kylepitcock.legacyconnector.vb1_7_3.MenusB173;

/** Client side of b1.7.3 GUIs: the menu screen, the drawing sink, and the client half of {@link MenusB173}. */
public final class ClientB173 implements MenusB173.Client {
    /** How long a GUI the client's callback built waits for its menu. */
    private static final long PENDING_MS = 10_000;

    private Screen pending;
    private long pendingAt;

    private ClientB173() {}

    public static void register(IEventBus modBus) {
        modBus.addListener((RegisterMenuScreensEvent e) -> {
            if (MenusB173.type() != null) e.register(MenusB173.type(), LegacyB173Screen::new);
        });
        GuiDrawB173.install(new ClientGuiB173());
        MenusB173.installClient(new ClientB173());
    }

    @Override
    public synchronized void openedOnClient(Screen gui) {
        Minecraft mc = Minecraft.getInstance();
        // single player: the server's own call opens it (with the GUI object the server built)
        if (mc.hasSingleplayerServer()) return;
        if (gui instanceof HandledScreen) {
            pending = gui;
            pendingAt = System.currentTimeMillis();
        } else {
            mc.execute(() -> mc.setScreen(new LegacyB173PlainScreen(gui)));
        }
    }

    @Override
    public void showFromServer(Screen gui) {
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> mc.setScreen(new LegacyB173PlainScreen(gui)));
    }

    @Override
    public synchronized Screen takePending(String guiClass) {
        Screen p = pending;
        if (p == null || !p.getClass().getName().equals(guiClass) || System.currentTimeMillis() - pendingAt > PENDING_MS) return null;
        pending = null;
        return p;
    }

    @Override
    public void setScreen(Screen screen) {
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> {
            if (screen == null) {
                if (mc.screen instanceof AbstractContainerScreen<?> && mc.player != null) mc.player.closeContainer();
                else mc.setScreen(null);
            } else {
                mc.setScreen(new LegacyB173PlainScreen(screen));
            }
        });
    }

    @Override
    public String clipboard() {
        try {
            return Minecraft.getInstance().keyboardHandler.getClipboard();
        } catch (RuntimeException e) {
            return "";
        }
    }
}
