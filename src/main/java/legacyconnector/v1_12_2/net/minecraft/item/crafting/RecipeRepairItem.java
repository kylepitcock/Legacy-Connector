package legacyconnector.v1_12_2.net.minecraft.item.crafting;

import java.util.ArrayList;
import java.util.List;
import legacyconnector.v1_12_2.net.minecraft.inventory.InventoryCrafting;
import legacyconnector.v1_12_2.net.minecraft.item.Item;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/** 1.12.2's repair recipe: two damaged items of the same repairable kind make one with their durability plus 5%. */
public class RecipeRepairItem extends legacyconnector.v1_12_2.net.minecraftforge.registries.IForgeRegistryEntry.Impl<IRecipe> implements IRecipe {
    private static List<ItemStack> pair(InventoryCrafting inv) {
        List<ItemStack> list = new ArrayList<>();
        for (int i = 0; i < inv.getSizeInventory(); ++i) {
            ItemStack stack = inv.getStackInSlot(i);
            if (stack.isEmpty()) continue;
            list.add(stack);
            if (list.size() > 1) {
                ItemStack first = list.get(0);
                if (stack.getItem() != first.getItem() || first.getCount() != 1 || stack.getCount() != 1 || !first.getItem().isRepairable()) return null;
            }
        }
        return list.size() == 2 ? list : null;
    }

    @Override
    public boolean matches(InventoryCrafting inv, World worldIn) {
        return pair(inv) != null;
    }

    @Override
    public ItemStack getCraftingResult(InventoryCrafting inv) {
        List<ItemStack> list = pair(inv);
        if (list == null) return ItemStack.EMPTY;
        ItemStack a = list.get(0);
        ItemStack b = list.get(1);
        Item item = a.getItem();
        int max = a.getMaxDamage();
        int durability = max - a.getItemDamage() + (max - b.getItemDamage()) + max * 5 / 100;
        int damage = Math.max(0, max - durability);
        return new ItemStack(item, 1, damage);
    }

    @Override
    public ItemStack getRecipeOutput() {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean isDynamic() {
        return true;
    }

    @Override
    public boolean canFit(int width, int height) {
        return width * height >= 2;
    }
}
