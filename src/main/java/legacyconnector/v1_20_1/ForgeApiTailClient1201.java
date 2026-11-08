package legacyconnector.v1_20_1;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.CustomizeGuiOverlayEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderHighlightEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;

/** Client-only part of {@link ForgeApiTail1201} (separate so a dedicated server never loads the client event classes); reached with a {@code TailClient.} prefix. */
public final class ForgeApiTailClient1201 {
    private ForgeApiTailClient1201() {}


    // ---- client events

    private static float partial(DeltaTracker tracker) {
        return tracker.getGameTimeDeltaPartialTick(false);
    }

    public static float guiPartialTick(RenderGuiEvent event) {
        return partial(event.getPartialTick());
    }

    public static float highlightPartialTick(RenderHighlightEvent event) {
        return partial(event.getDeltaTracker());
    }

    public static com.mojang.blaze3d.platform.Window guiWindow(RenderGuiEvent event) {
        return Minecraft.getInstance().getWindow();
    }

    public static double scrollDelta(ScreenEvent.MouseScrolled event) {
        return event.getScrollDeltaY();
    }

    /** The debug screen lists are ArrayLists in practice; a different list type is copied (and edits to the copy are lost). */
    public static ArrayList<String> debugLeft(CustomizeGuiOverlayEvent.DebugText event) {
        List<String> l = event.getLeft();
        return l instanceof ArrayList<String> a ? a : new ArrayList<>(l);
    }

    public static ArrayList<String> debugRight(CustomizeGuiOverlayEvent.DebugText event) {
        List<String> l = event.getRight();
        return l instanceof ArrayList<String> a ? a : new ArrayList<>(l);
    }

    // ---- exact-owner overloads (the rewritten call passes the receiver typed as the class the mod called it on)

    public static float guiPartialTick(RenderGuiEvent.Post e) { return guiPartialTick((RenderGuiEvent) e); }

    public static float guiPartialTick(RenderGuiEvent.Pre e) { return guiPartialTick((RenderGuiEvent) e); }

    public static com.mojang.blaze3d.platform.Window guiWindow(RenderGuiEvent.Post e) { return guiWindow((RenderGuiEvent) e); }

    public static com.mojang.blaze3d.platform.Window guiWindow(RenderGuiEvent.Pre e) { return guiWindow((RenderGuiEvent) e); }

    public static float highlightPartialTick(RenderHighlightEvent.Block e) { return highlightPartialTick((RenderHighlightEvent) e); }

    public static double scrollDelta(ScreenEvent.MouseScrolled.Pre e) { return scrollDelta((ScreenEvent.MouseScrolled) e); }

    public static double scrollDelta(ScreenEvent.MouseScrolled.Post e) { return scrollDelta((ScreenEvent.MouseScrolled) e); }}
