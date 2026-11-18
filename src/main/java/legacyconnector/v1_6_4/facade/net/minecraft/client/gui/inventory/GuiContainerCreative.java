package legacyconnector.v1_6_4.facade.net.minecraft.client.gui.inventory;

/** Facade of the 1.7.10 shim's {@code GuiContainerCreative} for 1.6.4 mods. See {@code Hierarchy.facadeOf}. Never instantiated. */
public final class GuiContainerCreative {
    private GuiContainerCreative() {}

    /** 1.6.4 {@code getCurrentTabIndex()}: the index of the creative tab on show (1.7.10: {@code func_147056_g}). */
    public static int getCurrentTabIndex(legacyconnector.v1_7_10.net.minecraft.client.gui.inventory.GuiContainerCreative self) {
        return self.func_147056_g();
    }
}
