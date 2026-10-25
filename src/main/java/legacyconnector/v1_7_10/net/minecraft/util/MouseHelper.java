package legacyconnector.v1_7_10.net.minecraft.util;

/** Mouse movement since the last frame (1.7.10 grabbed the mouse through this). */
public class MouseHelper {
    public int deltaX;
    public int deltaY;

    /** Captures the mouse again for playing (no screen open). */
    public void grabMouseCursor() {
        var mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.screen == null && mc.isWindowActive()) mc.mouseHandler.grabMouse();
    }

    /**
     * Frees the cursor (centred) while playing: mods do this for in-game menus picked with the mouse (Thaumcraft's focus
     * radial, held on the focus key).
     */
    public void ungrabMouseCursor() {
        net.minecraft.client.Minecraft.getInstance().mouseHandler.releaseMouse();
    }

    public void mouseXYChange() {
    }
}
