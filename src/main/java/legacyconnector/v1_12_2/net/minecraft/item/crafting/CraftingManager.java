package legacyconnector.v1_12_2.net.minecraft.item.crafting;

import legacyconnector.v1_12_2.net.minecraft.inventory.InventoryCrafting;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;
import legacyconnector.v1_12_2.net.minecraft.world.World;
import legacyconnector.v1_12_2.net.minecraft.util.registry.RegistryNamespaced;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.Registries1122;

/** The recipe registry as 1.12 mods read it. */
public class CraftingManager {
    public static final RegistryNamespaced<ResourceLocation, IRecipe> REGISTRY = Registries1122.view(Registries1122.RECIPES, "air");

    public static IRecipe getRecipe(ResourceLocation name) {
        return REGISTRY.getObject(name);
    }

    public static ResourceLocation getNameForRecipe(IRecipe recipe) {
        return REGISTRY.getNameForObject(recipe);
    }

    /** The first registered recipe that matches the grid, or null. */
    public static IRecipe findMatchingRecipe(InventoryCrafting craftMatrix, World worldIn) {
        for (IRecipe recipe : REGISTRY) {
            try {
                if (recipe.matches(craftMatrix, worldIn)) return recipe;
            } catch (RuntimeException | LinkageError e) {
                // a recipe that cannot judge this grid does not match
            }
        }
        return null;
    }

    public static ItemStack findMatchingResult(InventoryCrafting craftMatrix, World worldIn) {
        IRecipe recipe = findMatchingRecipe(craftMatrix, worldIn);
        return recipe == null ? ItemStack.EMPTY : recipe.getCraftingResult(craftMatrix);
    }
}
