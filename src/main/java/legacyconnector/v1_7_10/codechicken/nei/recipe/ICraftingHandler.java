package legacyconnector.v1_7_10.codechicken.nei.recipe;

public interface ICraftingHandler extends IRecipeHandler {
    ICraftingHandler getRecipeHandler(String outputId, Object... results);
}
