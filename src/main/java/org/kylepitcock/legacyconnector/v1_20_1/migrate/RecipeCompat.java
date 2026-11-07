package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import java.util.List;
import java.util.Optional;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

/**
 * Recipe calls of 1.20.1 code: recipes are matched against a {@code Container} there and against a {@code RecipeInput} here, and the recipe manager hands out
 * {@code RecipeHolder}s (a recipe plus its id) instead of recipes. (The overrides of a mod's own recipe classes are bridge methods, see
 * {@code mc-bridges.tsv}; {@link BridgeHelpers} converts the argument.)
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public final class RecipeCompat {
    private RecipeCompat() {}

    public static boolean matches(Recipe recipe, Container container, Level level) {
        return recipe.matches(BridgeHelpers.input(container), level);
    }

    public static ItemStack assemble(Recipe recipe, Container container, RegistryAccess access) {
        MigrateContext.remember(access);
        return recipe.assemble(BridgeHelpers.input(container), access);
    }

    public static ItemStack getResultItem(Recipe recipe, RegistryAccess access) {
        MigrateContext.remember(access);
        return recipe.getResultItem(access);
    }

    public static Optional getRecipeFor(RecipeManager manager, RecipeType type, Container container, Level level) {
        Optional<RecipeHolder> found = manager.getRecipeFor(type, BridgeHelpers.input(container), level);
        return found.map(RecipeHolder::value);
    }

    public static List getRecipesFor(RecipeManager manager, RecipeType type, Container container, Level level) {
        List<RecipeHolder> found = manager.getRecipesFor(type, BridgeHelpers.input(container), level);
        return found.stream().map(RecipeHolder::value).toList();
    }

    /** {@code Recipe.getId()}: recipes do not know their id in 1.21.1; it is found through the server's recipe manager (null for a recipe it does not hold). */
    public static net.minecraft.resources.ResourceLocation getId(net.minecraft.world.item.crafting.Recipe<?> recipe) {
        net.minecraft.server.MinecraftServer server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server == null) return null;
        for (net.minecraft.world.item.crafting.RecipeHolder<?> h : server.getRecipeManager().getRecipes()) if (h.value() == recipe) return h.id();
        return null;
    }
}
