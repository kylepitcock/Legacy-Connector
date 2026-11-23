package legacyconnector.v1_12_2.mezz.jei.api.gui;

/** JEI 4 (Minecraft 1.12.2) API: lays out a 3x3 grid. Legacy plugins run on the modern JEI through org.kylepitcock.legacyconnector.compat.jei.Jei1122Bridge. */
public interface ICraftingGridHelper {
    void setInputs(IGuiItemStackGroup guiItemStacks, java.util.List<java.util.List<legacyconnector.v1_12_2.net.minecraft.item.ItemStack>> input);
    void setInputs(IGuiItemStackGroup guiItemStacks, java.util.List<java.util.List<legacyconnector.v1_12_2.net.minecraft.item.ItemStack>> input, int width, int height);
}
