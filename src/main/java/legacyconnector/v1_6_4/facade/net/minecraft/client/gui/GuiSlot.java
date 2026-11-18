package legacyconnector.v1_6_4.facade.net.minecraft.client.gui;

/** Facade of the 1.7.10 shim's {@code GuiSlot} for 1.4.7 and 1.5.2 mods. See {@code Hierarchy.facadeOf}. Never instantiated. */
public final class GuiSlot {
    private GuiSlot() {}

    /**
     * {@code registerScrollButtons(buttonList, upId, downId)}: the list had the buttons added to it in the original, which the 1.7.10 slot does not need (the screen
     * hands the buttons' clicks to the slot); the two ids are what the slot keeps.
     */
    public static void registerScrollButtons(legacyconnector.v1_7_10.net.minecraft.client.gui.GuiSlot self, java.util.List buttons, int upId, int downId) {
        self.registerScrollButtons(upId, downId);
    }
}
