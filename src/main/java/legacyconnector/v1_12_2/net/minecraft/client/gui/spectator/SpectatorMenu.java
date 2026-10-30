package legacyconnector.v1_12_2.net.minecraft.client.gui.spectator;

/**
 * 1.12.2 spectator hotbar menu (teleport to a player or team). The modern spectator GUI keeps its own menu; this is the legacy type
 * {@code GuiSpectator.onSpectatorMenuClosed} takes, closing it closes the modern one.
 */
public class SpectatorMenu {
    private boolean closed;

    public void exit() {
        if (closed) return;
        closed = true;
        var mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.gui != null) mc.gui.getSpectatorGui().onSpectatorMenuClosed(null);
    }
}
