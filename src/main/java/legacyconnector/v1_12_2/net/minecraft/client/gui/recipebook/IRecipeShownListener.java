package legacyconnector.v1_12_2.net.minecraft.client.gui.recipebook;

/** A screen with a recipe book (the inventory, the crafting table, mods' crafting screens): JEI and others read the book's area from it. */
public interface IRecipeShownListener {
    void recipesUpdated();

    GuiRecipeBook func_194310_f();
}
