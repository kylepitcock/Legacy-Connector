package legacyconnector.v1_12_2.mezz.jei.api.ingredients;

/** JEI 4 (Minecraft 1.12.2) API: a recipe's ingredients. Legacy plugins run on the modern JEI through org.kylepitcock.legacyconnector.compat.jei.Jei1122Bridge. */
public interface IIngredients {
    <T> void setInput(legacyconnector.v1_12_2.mezz.jei.api.recipe.IIngredientType<T> ingredientType, T input);
    <T> void setInputs(legacyconnector.v1_12_2.mezz.jei.api.recipe.IIngredientType<T> ingredientType, java.util.List<T> input);
    <T> void setInputLists(legacyconnector.v1_12_2.mezz.jei.api.recipe.IIngredientType<T> ingredientType, java.util.List<java.util.List<T>> inputs);
    <T> void setOutput(legacyconnector.v1_12_2.mezz.jei.api.recipe.IIngredientType<T> ingredientType, T output);
    <T> void setOutputs(legacyconnector.v1_12_2.mezz.jei.api.recipe.IIngredientType<T> ingredientType, java.util.List<T> outputs);
    <T> void setOutputLists(legacyconnector.v1_12_2.mezz.jei.api.recipe.IIngredientType<T> ingredientType, java.util.List<java.util.List<T>> outputs);
    <T> java.util.List<java.util.List<T>> getInputs(legacyconnector.v1_12_2.mezz.jei.api.recipe.IIngredientType<T> ingredientType);
    <T> java.util.List<java.util.List<T>> getOutputs(legacyconnector.v1_12_2.mezz.jei.api.recipe.IIngredientType<T> ingredientType);
    void setInputIngredients(java.util.List<legacyconnector.v1_12_2.net.minecraft.item.crafting.Ingredient> inputs);
    <T> void setInput(Class<? extends T> ingredientClass, T input);
    <T> void setInputs(Class<? extends T> ingredientClass, java.util.List<T> input);
    <T> void setInputLists(Class<? extends T> ingredientClass, java.util.List<java.util.List<T>> inputs);
    <T> void setOutput(Class<? extends T> ingredientClass, T output);
    <T> void setOutputs(Class<? extends T> ingredientClass, java.util.List<T> outputs);
    <T> void setOutputLists(Class<? extends T> ingredientClass, java.util.List<java.util.List<T>> outputs);
    <T> java.util.List<java.util.List<T>> getInputs(Class<? extends T> ingredientClass);
    <T> java.util.List<java.util.List<T>> getOutputs(Class<? extends T> ingredientClass);
}
