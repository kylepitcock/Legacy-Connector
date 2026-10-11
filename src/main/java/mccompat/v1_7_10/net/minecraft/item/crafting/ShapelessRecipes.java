package mccompat.v1_7_10.net.minecraft.item.crafting;

import java.util.ArrayList;
import java.util.List;
import mccompat.v1_7_10.net.minecraft.inventory.InventoryCrafting;
import mccompat.v1_7_10.net.minecraft.item.ItemStack;
import mccompat.v1_7_10.net.minecraft.world.World;

public class ShapelessRecipes implements IRecipe {
    private final ItemStack recipeOutput;
    @SuppressWarnings("rawtypes")
    public final List recipeItems;

    @SuppressWarnings("rawtypes")
    public ShapelessRecipes(ItemStack output, List items) {
        this.recipeOutput = output;
        this.recipeItems = items;
    }

    @Override
    public ItemStack getRecipeOutput() {
        return recipeOutput;
    }

    @Override
    public boolean matches(InventoryCrafting inv, World world) {
        List<Object> remaining = new ArrayList<>(recipeItems);
        for (int i = 0; i < inv.getSizeInventory(); i++) {
            ItemStack s = inv.getStackInSlot(i);
            if (s == null) continue;
            boolean found = false;
            for (Object o : remaining) {
                ItemStack e = (ItemStack) o;
                if (s.getItem() == e.getItem() && (e.getItemDamage() == 32767 || s.getItemDamage() == e.getItemDamage())) {
                    remaining.remove(o);
                    found = true;
                    break;
                }
            }
            if (!found) return false;
        }
        return remaining.isEmpty();
    }

    @Override
    public ItemStack getCraftingResult(InventoryCrafting inv) {
        return recipeOutput.copy();
    }

    @Override
    public int getRecipeSize() {
        return recipeItems.size();
    }
}
