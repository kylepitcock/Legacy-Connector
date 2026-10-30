package legacyconnector.v1_12_2.net.minecraft.item.crafting;

import legacyconnector.v1_12_2.net.minecraft.init.Items;
import legacyconnector.v1_12_2.net.minecraft.inventory.InventoryCrafting;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.util.NonNullList;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/** 1.12.2's dynamic map copying: a filled map and empty maps make that many more copies of the filled map. */
public class RecipesMapCloning extends legacyconnector.v1_12_2.net.minecraftforge.registries.IForgeRegistryEntry.Impl<IRecipe> implements IRecipe {
    @Override
    public boolean matches(InventoryCrafting inv, World worldIn) {
        return !getCraftingResult(inv).isEmpty();
    }

    @Override
    public ItemStack getCraftingResult(InventoryCrafting inv) {
        int empty = 0;
        ItemStack filled = ItemStack.EMPTY;
        for (int i = 0; i < inv.getSizeInventory(); ++i) {
            ItemStack stack = inv.getStackInSlot(i);
            if (stack.isEmpty()) continue;
            if (stack.getItem() == Items.FILLED_MAP) {
                if (!filled.isEmpty()) return ItemStack.EMPTY;
                filled = stack;
            } else if (stack.getItem() == Items.MAP) {
                ++empty;
            } else {
                return ItemStack.EMPTY;
            }
        }
        if (filled.isEmpty() || empty < 1) return ItemStack.EMPTY;
        ItemStack copy = new ItemStack(Items.FILLED_MAP, empty + 1, filled.getMetadata());
        if (filled.hasDisplayName()) copy.setStackDisplayName(filled.getDisplayName());
        if (filled.hasTagCompound()) copy.setTagCompound(filled.getTagCompound().copy());
        return copy;
    }

    @Override
    public ItemStack getRecipeOutput() {
        return ItemStack.EMPTY;
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(InventoryCrafting inv) {
        NonNullList<ItemStack> list = NonNullList.withSize(inv.getSizeInventory(), ItemStack.EMPTY);
        for (int i = 0; i < list.size(); ++i) {
            ItemStack stack = inv.getStackInSlot(i);
            if (!stack.isEmpty() && stack.getItem().hasContainerItem(stack)) list.set(i, stack.getItem().getContainerItem(stack));
        }
        return list;
    }

    @Override
    public boolean isDynamic() {
        return true;
    }

    @Override
    public boolean canFit(int width, int height) {
        return width >= 3 && height >= 3;
    }
}
