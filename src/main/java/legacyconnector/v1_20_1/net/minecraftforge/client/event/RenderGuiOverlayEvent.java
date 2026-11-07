package legacyconnector.v1_20_1.net.minecraftforge.client.event;

import com.mojang.blaze3d.platform.Window;
import legacyconnector.v1_20_1.net.minecraftforge.client.gui.overlay.NamedGuiOverlay;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

/** Forge 1.20.1's per-overlay render events; posted from NeoForge's {@code RenderGuiLayerEvent.Pre/Post} for the layers that stand for a vanilla overlay. */
public abstract class RenderGuiOverlayEvent extends Event {
    private final Window window;
    private final GuiGraphics guiGraphics;
    private final float partialTick;
    private final NamedGuiOverlay overlay;

    protected RenderGuiOverlayEvent(Window window, GuiGraphics guiGraphics, float partialTick, NamedGuiOverlay overlay) {
        this.window = window;
        this.guiGraphics = guiGraphics;
        this.partialTick = partialTick;
        this.overlay = overlay;
    }

    public Window getWindow() {
        return window;
    }

    public GuiGraphics getGuiGraphics() {
        return guiGraphics;
    }

    public float getPartialTick() {
        return partialTick;
    }

    public NamedGuiOverlay getOverlay() {
        return overlay;
    }

    public static class Pre extends RenderGuiOverlayEvent implements ICancellableEvent {
        public Pre(Window window, GuiGraphics guiGraphics, float partialTick, NamedGuiOverlay overlay) {
            super(window, guiGraphics, partialTick, overlay);
        }
    }

    public static class Post extends RenderGuiOverlayEvent {
        public Post(Window window, GuiGraphics guiGraphics, float partialTick, NamedGuiOverlay overlay) {
            super(window, guiGraphics, partialTick, overlay);
        }
    }
}
