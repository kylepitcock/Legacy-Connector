package org.kylepitcock.legacyconnector.vb1_7_3.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.GenericMessageScreen;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.gui.screens.ProgressScreen;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.neoforge.client.event.ScreenEvent;
import org.kylepitcock.legacyconnector.core.synth.Synth;
import org.slf4j.LoggerFactory;

/**
 * A b1.7.3 mod can replace the game's progress renderer ({@code Minecraft.progressRenderer}) to draw its own loading screen. The native
 * loading screens call that renderer's stage and percentage methods and let it draw over them, with the same immediate-mode drawing a
 * legacy GUI uses. Without an installed renderer the native screens are untouched.
 */
public final class LoadingProgressB173 {
    private static final String RENDERER = "net.minecraft.client.Minecraft.progressRenderer";
    private static final String RUNNING = "net.minecraft.client.Minecraft.running";
    /** The strings Beta's own loading screen showed; the native screens show their own equivalents. */
    private static final String TITLE = "Loading level", STAGE = "Building terrain";
    private static Screen current;
    private static boolean failed;

    private LoadingProgressB173() {}

    public static void register() {
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener((ScreenEvent.Render.Post event) -> render(event.getScreen(), event.getGuiGraphics()));
    }

    static boolean loadingScreen(Screen screen) {
        return screen instanceof LevelLoadingScreen || screen instanceof ReceivingLevelScreen
                || screen instanceof GenericMessageScreen || screen instanceof ProgressScreen;
    }

    private static void render(Screen screen, GuiGraphics graphics) {
        if (failed || !loadingScreen(screen)) return;
        var minecraft = legacyconnector.vb1_7_3.net.minecraft.client.Minecraft.mcc$instance();
        Object renderer = Synth.getField(minecraft, RENDERER, null);
        if (renderer == null) return;
        try {
            var window = net.minecraft.client.Minecraft.getInstance().getWindow();
            minecraft.displayWidth = window.getWidth();
            minecraft.displayHeight = window.getHeight();
            // The renderer stops drawing while the legacy game is not "running"; the modern game is.
            Synth.setField(minecraft, RUNNING, Boolean.TRUE);
            if (screen != current) {
                current = screen;
                call(renderer, "progressStartNoAbort", TITLE);
                call(renderer, "progressStage", STAGE);
            }
            // Only what the renderer adds to the standard title and stage is drawn, over the native screen.
            ClientGuiB173.beginOverlay(graphics, java.util.Set.of(TITLE, STAGE));
            try {
                // Beta redrew the loading screen only a few times a second and the picture stayed on screen between draws; the native screen
                // is redrawn every frame, so the renderer's own rate limit (it skips a draw within 20 ms of the last) would leave some frames
                // without its additions, which shows as flicker on a fast display.
                resetRateLimit(renderer);
                renderer.getClass().getMethod("progressStagePercentage", int.class).invoke(renderer, percent(screen));
            } finally {
                ClientGuiB173.end();
            }
        } catch (ReflectiveOperationException | RuntimeException | LinkageError error) {
            failed = true;
            LoggerFactory.getLogger("LegacyConnector/b1.7.3").error("b1.7.3 loading screen {} failed; the native loading screens stay",
                    renderer.getClass().getName(), error);
        }
    }

    /** The renderer's last-update timestamp(s): its {@code long} fields, which Beta's progress renderer used for nothing but that limit. */
    private static void resetRateLimit(Object renderer) {
        for (Class<?> type = renderer.getClass(); type != null && type != Object.class; type = type.getSuperclass()) {
            for (var field : type.getDeclaredFields()) {
                if (field.getType() != long.class || java.lang.reflect.Modifier.isStatic(field.getModifiers())) continue;
                try { field.setAccessible(true); field.setLong(renderer, 0L); } catch (ReflectiveOperationException | RuntimeException ignored) { }
            }
        }
    }

    private static void call(Object renderer, String method, String text) throws ReflectiveOperationException {
        renderer.getClass().getMethod(method, String.class).invoke(renderer, text);
    }

    /** The chunk preparation progress in percent, or -1 (no bar) where the native screen has none. */
    private static int percent(Screen screen) {
        if (screen instanceof LevelLoadingScreen loading) {
            try {
                var field = LevelLoadingScreen.class.getDeclaredField("progressListener");
                field.setAccessible(true);
                return ((net.minecraft.server.level.progress.StoringChunkProgressListener) field.get(loading)).getProgress();
            } catch (ReflectiveOperationException | RuntimeException ignored) {
                // no bar
            }
        }
        return -1;
    }
}
