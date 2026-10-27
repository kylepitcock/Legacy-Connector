package legacyconnector.v1_12_2.net.minecraft.client.gui.toasts;

import legacyconnector.v1_12_2.net.minecraft.client.Minecraft;
import legacyconnector.v1_12_2.net.minecraft.client.gui.Gui;

/** The toast stack ({@code Minecraft.getToastGui()}): legacy toasts are shown by the modern toast manager and drawn by their own code. */
public class GuiToast extends Gui {
    public static final GuiToast mcc$INSTANCE = new GuiToast(null);

    public GuiToast(Minecraft mcIn) {
    }

    public void add(IToast toastIn) {
        if (toastIn != null) org.kylepitcock.legacyconnector.v1_12_2.client.LegacyToasts1122.add(this, toastIn);
    }

    public <T extends IToast> T getToast(Class<? extends T> toastClass, Object type) {
        return org.kylepitcock.legacyconnector.v1_12_2.client.LegacyToasts1122.find(toastClass, type);
    }

    public void clear() {
        net.minecraft.client.Minecraft.getInstance().getToasts().clear();
    }

    /** Draws the toasts showing (the game draws them every frame itself; mods call this to draw them over their own screens). */
    public void drawToast(legacyconnector.v1_12_2.net.minecraft.client.gui.ScaledResolution resolution) {
        var gui = org.kylepitcock.legacyconnector.v1_7_10.client.RenderContext.gui();
        if (gui != null) net.minecraft.client.Minecraft.getInstance().getToasts().render(gui);
    }

    public Minecraft getMinecraft() {
        return Minecraft.getMinecraft();
    }
}
