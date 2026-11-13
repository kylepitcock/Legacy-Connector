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

    /** The members of the 1.20.1 interface a data generator reads; the recipe is not generated at run time, so the defaults only keep callers linking. */
    default net.minecraft.resources.ResourceLocation getId() {
        return id();
    }

    default net.minecraft.world.item.crafting.RecipeSerializer<?> getType() {
        return null;
    }

    default com.google.gson.JsonObject serializeAdvancement() {
        return null;
    }

    default net.minecraft.resources.ResourceLocation getAdvancementId() {
        return null;
    }
}
