package legacyconnector.v1_7_10.codechicken.nei.recipe;

import java.util.ArrayList;

/** NEI's usage lookup screen (see {@link GuiCraftingRecipe}). */
public class GuiUsageRecipe {
    public static ArrayList<IUsageHandler> usagehandlers = new ArrayList<>();
    public static ArrayList<IUsageHandler> serialUsageHandlers = new ArrayList<>();

    public static boolean openRecipeGui(String inputId, Object... ingredients) {
        return false;
    }

    public static void registerUsageHandler(IUsageHandler handler) {
        usagehandlers.add(handler);
    }
}
