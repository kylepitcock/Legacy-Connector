package legacyconnector.v1_7_10.codechicken.nei.recipe;

import java.util.HashMap;
import java.util.Map;
import legacyconnector.v1_7_10.codechicken.nei.api.IOverlayHandler;
import legacyconnector.v1_7_10.net.minecraft.client.gui.inventory.GuiContainer;

/** NEI's per-GUI recipe settings, recorded only. */
public class RecipeInfo {
    public static final Map<Class<?>, Map<String, IOverlayHandler>> overlayMap = new HashMap<>();

    public static void registerOverlayHandler(Class<? extends GuiContainer> gui, IOverlayHandler handler, String ident) {
        overlayMap.computeIfAbsent(gui, k -> new HashMap<>()).put(ident, handler);
    }

    /** NEI 1.4.7 / 1.5.2: where a GUI class's overlay puts the ghost ingredients (recorded; JEI draws its own transfer). */
    public static final Map<Class<?>, Map<String, legacyconnector.v1_7_10.codechicken.nei.api.IStackPositioner>> positionerMap = new HashMap<>();

    public static void registerGuiOverlay(Class<? extends GuiContainer> gui, String ident, legacyconnector.v1_7_10.codechicken.nei.api.IStackPositioner positioner) {
        positionerMap.computeIfAbsent(gui, k -> new HashMap<>()).put(ident, positioner);
    }

    public static legacyconnector.v1_7_10.codechicken.nei.api.IStackPositioner getStackPositioner(GuiContainer gui, String ident) {
        Map<String, legacyconnector.v1_7_10.codechicken.nei.api.IStackPositioner> map = positionerMap.get(gui == null ? null : gui.getClass());
        return map == null ? null : map.get(ident);
    }

    public static IOverlayHandler getOverlayHandler(GuiContainer gui, String ident) {
        Map<String, IOverlayHandler> map = overlayMap.get(gui == null ? null : gui.getClass());
        return map == null ? null : map.get(ident);
    }

    /** Whether an overlay handler is registered for the GUI class and handler id. */
    public static boolean hasOverlayHandler(GuiContainer gui, String ident) {
        return getOverlayHandler(gui, ident) != null;
    }

    public static boolean hasDefaultOverlay(GuiContainer gui, String ident) {
        return getOverlayHandler(gui, ident) != null;
    }

    public static int[] getGuiOffset(GuiContainer gui) {
        return new int[] {5, 11};
    }
}
