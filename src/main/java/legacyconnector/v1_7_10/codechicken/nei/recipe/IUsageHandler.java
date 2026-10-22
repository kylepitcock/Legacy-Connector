package legacyconnector.v1_7_10.codechicken.nei.recipe;

public interface IUsageHandler extends IRecipeHandler {
    IUsageHandler getUsageHandler(String inputId, Object... ingredients);
}
