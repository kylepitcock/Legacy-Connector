package legacyconnector.v1_6_4.facade.net.minecraft.client.gui;

/** Facade of the 1.7.10 shim's {@code GuiButton} for 1.6.4 mods. See {@code Hierarchy.facadeOf}. Never instantiated. */
public final class GuiButton {
    private GuiButton() {}

    /** 1.6.4 {@code field_82253_i}: whether the mouse is over the button (set while it draws). */
    public static boolean mcc$get$field_82253_i(legacyconnector.v1_7_10.net.minecraft.client.gui.GuiButton self) {
        return self.isMouseOver();
    }
}
