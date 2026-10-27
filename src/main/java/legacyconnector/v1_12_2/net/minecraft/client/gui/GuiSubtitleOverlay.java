package legacyconnector.v1_12_2.net.minecraft.client.gui;

import legacyconnector.v1_12_2.net.minecraft.client.Minecraft;

/**
 * The sound subtitles in the bottom right corner. The game draws its own; {@link #renderSubtitles} draws the modern overlay into the
 * current GUI (mods that replace the overlay subclass this and draw their own).
 */
public class GuiSubtitleOverlay extends Gui {
    private final Minecraft client;
    private boolean enabled;

    public GuiSubtitleOverlay(Minecraft clientIn) {
        this.client = clientIn;
    }

    public void renderSubtitles(ScaledResolution resolution) {
        var gui = org.kylepitcock.legacyconnector.v1_7_10.client.RenderContext.gui();
        if (gui == null) return;
        var mc = net.minecraft.client.Minecraft.getInstance();
        enabled = mc.options.showSubtitles().get();
        Object overlay = org.kylepitcock.legacyconnector.v1_12_2.client.Fields1122.get(net.minecraft.client.gui.Gui.class, mc.gui, "subtitleOverlay");
        if (overlay instanceof net.minecraft.client.gui.components.SubtitleOverlay subtitles) subtitles.render(gui);
    }
}
