package legacyconnector.v1_12_2.mezz.jei.api.gui;

/** JEI 4 (Minecraft 1.12.2) API: areas JEI's overlay keeps clear of, and ingredients under the mouse. Legacy plugins run on the modern JEI through org.kylepitcock.legacyconnector.compat.jei.Jei1122Bridge. */
public interface IAdvancedGuiHandler<T extends legacyconnector.v1_12_2.net.minecraft.client.gui.inventory.GuiContainer> {
    Class<T> getGuiContainerClass();
    default java.util.List<java.awt.Rectangle> getGuiExtraAreas(T guiContainer) {
        return null;
    }
    default Object getIngredientUnderMouse(T guiContainer, int mouseX, int mouseY) {
        return null;
    }
}
