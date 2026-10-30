package legacyconnector.v1_12_2.net.minecraft.client.gui;

import legacyconnector.v1_12_2.net.minecraft.client.Minecraft;
import legacyconnector.v1_12_2.net.minecraft.client.gui.spectator.SpectatorMenu;

/**
 * 1.12.2 spectator hotbar (the menu spectators open with the number keys). Mapped onto the HUD's modern spectator GUI: the menu, its
 * scrolling and selection are the game's own, drawn by the modern HUD.
 */
public class GuiSpectator extends Gui {
    public GuiSpectator(Minecraft mcIn) {
    }

    private static net.minecraft.client.gui.components.spectator.SpectatorGui modern() {
        var gui = net.minecraft.client.Minecraft.getInstance().gui;
        return gui == null ? null : gui.getSpectatorGui();
    }

    /** Selects a hotbar slot of the menu (opening it first if closed). */
    public void onHotbarSelected(int slot) {
        var m = modern();
        if (m != null) m.onHotbarSelected(slot);
    }

    /** Closes the menu. */
    public void onSpectatorMenuClosed(SpectatorMenu menu) {
        var m = modern();
        if (m != null) m.onSpectatorMenuClosed(null);
    }

    public boolean isMenuActive() {
        var m = modern();
        return m != null && m.isMenuActive();
    }

    public void onMouseScroll(int amount) {
        var m = modern();
        if (m != null) m.onMouseScrolled(amount);
    }

    public void onMiddleClick() {
        var m = modern();
        if (m != null) m.onMouseMiddleClick();
    }

    /** The modern HUD draws the spectator hotbar and its tooltip itself. */
    public void renderTooltip(ScaledResolution scaledRes, float partialTicks) {
    }

    public void renderSelectedItem(ScaledResolution scaledRes) {
    }
}
