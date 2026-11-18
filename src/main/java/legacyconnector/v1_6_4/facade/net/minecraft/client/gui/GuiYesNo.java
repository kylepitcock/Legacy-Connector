package legacyconnector.v1_6_4.facade.net.minecraft.client.gui;

/** Facade of the 1.7.10 shim's {@code GuiYesNo} for 1.6.4 mods. See {@code Hierarchy.facadeOf}. Never instantiated. */
public final class GuiYesNo {
    private GuiYesNo() {}

    /**
     * 1.6.4 {@code parentScreen} (the screen that asked and gets the answer): 1.7.10 keeps it as the {@code GuiYesNoCallback}, which is the screen itself
     * in every vanilla and mod use, so the callback is handed out when it is a screen.
     */
    public static legacyconnector.v1_7_10.net.minecraft.client.gui.GuiScreen mcc$get$parentScreen(legacyconnector.v1_7_10.net.minecraft.client.gui.GuiYesNo self) {
        return self.mcc$callback() instanceof legacyconnector.v1_7_10.net.minecraft.client.gui.GuiScreen screen ? screen : null;
    }
}
