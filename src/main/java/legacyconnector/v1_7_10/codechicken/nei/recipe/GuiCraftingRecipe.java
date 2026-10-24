package legacyconnector.v1_7_10.codechicken.nei.recipe;

import java.util.ArrayList;

/** NEI's crafting recipe lookup screen; under JEI the lookup is JEI's, so opening one reports failure. */
public class GuiCraftingRecipe {
    public static ArrayList<ICraftingHandler> craftinghandlers = new ArrayList<>();
    public static ArrayList<ICraftingHandler> serialCraftingHandlers = new ArrayList<>();

    public static boolean openRecipeGui(String outputId, Object... results) {
        return false;
    }

    public static void registerRecipeHandler(ICraftingHandler handler) {
        craftinghandlers.add(handler);
    }
}
