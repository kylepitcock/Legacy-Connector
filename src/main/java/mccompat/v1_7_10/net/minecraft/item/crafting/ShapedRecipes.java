package mccompat.v1_7_10.net.minecraft.item.crafting;

import mccompat.v1_7_10.net.minecraft.inventory.InventoryCrafting;
import mccompat.v1_7_10.net.minecraft.item.ItemStack;
import mccompat.v1_7_10.net.minecraft.nbt.NBTTagCompound;
import mccompat.v1_7_10.net.minecraft.world.World;

/** Vanilla shaped recipe (exact item stacks; null = empty cell, metadata 32767 = any). */
public class ShapedRecipes implements IRecipe {
    public final int recipeWidth;
    public final int recipeHeight;
    public final ItemStack[] recipeItems;
    private final ItemStack recipeOutput;
    private boolean field_92101_f;

    public ShapedRecipes(int width, int height, ItemStack[] items, ItemStack output) {
        this.recipeWidth = width;
        this.recipeHeight = height;
        this.recipeItems = items;
        this.recipeOutput = output;
    }

    @Override
    public ItemStack getRecipeOutput() {
        return recipeOutput;
    }

    @Override
    public boolean matches(InventoryCrafting inv, World world) {
        for (int x = 0; x <= 3 - recipeWidth; x++) {
            for (int y = 0; y <= 3 - recipeHeight; y++) {
                if (checkMatch(inv, x, y, true) || checkMatch(inv, x, y, false)) return true;
            }
        }
        return false;
    }

    private boolean checkMatch(InventoryCrafting inv, int dx, int dy, boolean mirror) {
        for (int x = 0; x < 3; x++) {
            for (int y = 0; y < 3; y++) {
                int rx = x - dx;
                int ry = y - dy;
                ItemStack expected = null;
                if (rx >= 0 && ry >= 0 && rx < recipeWidth && ry < recipeHeight) {
                    expected = recipeItems[mirror ? recipeWidth - rx - 1 + ry * recipeWidth : rx + ry * recipeWidth];
                }
                ItemStack actual = inv.getStackInRowAndColumn(x, y);
                if (expected == null && actual == null) continue;
                if (expected == null || actual == null || expected.getItem() != actual.getItem()) return false;
                if (expected.getItemDamage() != 32767 && expected.getItemDamage() != actual.getItemDamage()) return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getCraftingResult(InventoryCrafting inv) {
        ItemStack out = getRecipeOutput().copy();
        if (field_92101_f) {
            for (int i = 0; i < inv.getSizeInventory(); i++) {
                ItemStack s = inv.getStackInSlot(i);
                if (s != null && s.hasTagCompound()) out.setTagCompound((NBTTagCompound) s.getTagCompound().copy());
            }
        }
        return out;
    }

    @Override
    public int getRecipeSize() {
        return recipeWidth * recipeHeight;
    }

    /** Copies the ingredients' NBT onto the result. */
    public ShapedRecipes func_92100_c() {
        field_92101_f = true;
        return this;
    }
}
