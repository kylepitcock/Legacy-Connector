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

    /** The result of the grid: a legacy recipe's, else a modern (vanilla or data pack) recipe's. */
    public static ItemStack findMatchingResult(InventoryCrafting craftMatrix, World worldIn) {
        IRecipe recipe = findMatchingRecipe(craftMatrix, worldIn);
        return recipe == null ? org.kylepitcock.legacyconnector.v1_12_2.bridge.Crafting1122.modernResult(craftMatrix, worldIn) : recipe.getCraftingResult(craftMatrix);
    }

    /** What crafting leaves in the grid: the matching recipe's remaining items, else each stack's container item (Forge's default). */
    public static legacyconnector.v1_12_2.net.minecraft.util.NonNullList<ItemStack> getRemainingItems(InventoryCrafting craftMatrix, World worldIn) {
        IRecipe recipe = findMatchingRecipe(craftMatrix, worldIn);
        if (recipe != null) return recipe.getRemainingItems(craftMatrix);
        var modern = org.kylepitcock.legacyconnector.v1_12_2.bridge.Crafting1122.modernRemaining(craftMatrix, worldIn);
        if (modern != null) return modern;
        legacyconnector.v1_12_2.net.minecraft.util.NonNullList<ItemStack> list = legacyconnector.v1_12_2.net.minecraft.util.NonNullList.withSize(craftMatrix.getSizeInventory(), ItemStack.EMPTY);
        for (int i = 0; i < list.size(); ++i) {
            ItemStack s = craftMatrix.getStackInSlot(i);
            if (s.isEmpty() || !s.getItem().hasContainerItem(s)) continue;
            ItemStack container = s.getItem().getContainerItem(s);
            if (!container.isEmpty() && container.isItemStackDamageable() && container.getMetadata() > container.getMaxDamage()) container = ItemStack.EMPTY;
            list.set(i, container);
        }
        return list;
    }
}
