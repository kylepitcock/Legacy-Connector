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

    public Minecraft getMinecraft() {
        return Minecraft.getMinecraft();
    }
}
