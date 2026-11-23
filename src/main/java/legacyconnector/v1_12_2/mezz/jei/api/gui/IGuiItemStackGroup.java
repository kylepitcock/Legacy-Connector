package legacyconnector.v1_12_2.mezz.jei.api.gui;

/** JEI 4 (Minecraft 1.12.2) API: the item slots. Legacy plugins run on the modern JEI through org.kylepitcock.legacyconnector.compat.jei.Jei1122Bridge. */
public interface IGuiItemStackGroup extends IGuiIngredientGroup<legacyconnector.v1_12_2.net.minecraft.item.ItemStack> {
    /** JEI 4 redeclared these with the item type, so mods call them by that descriptor. */
    @Override
    void set(int slotIndex, legacyconnector.v1_12_2.net.minecraft.item.ItemStack itemStack);

    @Override
    void set(int slotIndex, java.util.List<legacyconnector.v1_12_2.net.minecraft.item.ItemStack> itemStacks);
}
