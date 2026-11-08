package org.kylepitcock.legacyconnector.v1_20_1.migrate;

/** Stands in for {@code net.minecraft.data.recipes.FinishedRecipe} (1.20.1), the data generator's recipe output: data generation does not run, only the type is kept so classes link. */
public interface LegacyFinishedRecipe {
    default com.google.gson.JsonObject serializeRecipe() {
        return new com.google.gson.JsonObject();
    }

    default void serializeRecipeData(com.google.gson.JsonObject json) {}

    default net.minecraft.resources.ResourceLocation id() {
        return null;
    }
}
