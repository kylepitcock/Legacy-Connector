package legacyconnector.v1_20_1.net.minecraftforge.client.gui.overlay;

import java.util.List;

/** Forge 1.20.1's overlay list. The layer registers overlays straight with NeoForge's layer stack: the list is empty (mods only read it to look an overlay up). */
public final class GuiOverlayManager {
    private GuiOverlayManager() {
    }

    public static List<NamedGuiOverlay> getOverlays() {
        return List.of();
    }

    public static NamedGuiOverlay findOverlay(net.minecraft.resources.ResourceLocation id) {
        return null;
    }
}
