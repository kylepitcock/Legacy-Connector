package legacyconnector.v1_12_2.net.minecraft.item.crafting;

import legacyconnector.v1_12_2.net.minecraft.inventory.InventoryCrafting;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/** The 1.12.2 tipped arrow recipe: eight arrows around a lingering potion make eight tipped arrows with the potion's effects. */
public class RecipeTippedArrow extends legacyconnector.v1_12_2.net.minecraftforge.registries.IForgeRegistryEntry.Impl<IRecipe> implements IRecipe {
    public RecipeTippedArrow() {
    }

    private static String name(ItemStack stack) {
        return String.valueOf(stack.getItem().getRegistryName());
    }

    @Override
    public boolean matches(InventoryCrafting inv, World worldIn) {
        if (inv.getWidth() != 3 || inv.getHeight() != 3) return false;
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 3; ++j) {
                ItemStack stack = inv.getStackInRowAndColumn(i, j);
                if (stack.isEmpty()) return false;
                boolean center = i == 1 && j == 1;
                if (!name(stack).equals(center ? "minecraft:lingering_potion" : "minecraft:arrow")) return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getCraftingResult(InventoryCrafting inv) {
        ItemStack potion = inv.getStackInRowAndColumn(1, 1);
        if (potion.isEmpty() || !name(potion).equals("minecraft:lingering_potion")) return ItemStack.EMPTY;
        var arrowItem = legacyconnector.v1_12_2.net.minecraft.item.Item.REGISTRY.getObject(new legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation("minecraft:tipped_arrow"));
        if (arrowItem == null) return ItemStack.EMPTY;
        ItemStack result = new ItemStack(arrowItem, 8);
        if (potion.hasTagCompound()) result.setTagCompound(potion.getTagCompound().copy());
        return result;
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
        return width >= 3 && height >= 3;
    }
}
