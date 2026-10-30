package legacyconnector.v1_12_2.net.minecraft.item.crafting;

import java.util.ArrayList;
import java.util.List;
import legacyconnector.v1_12_2.net.minecraft.inventory.InventoryCrafting;
import legacyconnector.v1_12_2.net.minecraft.item.ItemArmor;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.util.NonNullList;
import legacyconnector.v1_12_2.net.minecraft.world.World;
import legacyconnector.v1_12_2.net.minecraftforge.oredict.DyeUtils;

/** 1.12.2's dynamic leather armour dyeing: one leather armour piece and dyes; the colours are mixed as in 1.12. */
public class RecipesArmorDyes extends legacyconnector.v1_12_2.net.minecraftforge.registries.IForgeRegistryEntry.Impl<IRecipe> implements IRecipe {
    @Override
    public boolean matches(InventoryCrafting inv, World worldIn) {
        ItemStack armor = ItemStack.EMPTY;
        List<ItemStack> dyes = new ArrayList<>();
        for (int i = 0; i < inv.getSizeInventory(); ++i) {
            ItemStack stack = inv.getStackInSlot(i);
            if (stack.isEmpty()) continue;
            if (stack.getItem() instanceof ItemArmor a) {
                if (a.getArmorMaterial() != ItemArmor.ArmorMaterial.LEATHER || !armor.isEmpty()) return false;
                armor = stack;
            } else {
                if (!DyeUtils.isDye(stack)) return false;
                dyes.add(stack);
            }
        }
        return !armor.isEmpty() && !dyes.isEmpty();
    }

    @Override
    public ItemStack getCraftingResult(InventoryCrafting inv) {
        ItemStack result = ItemStack.EMPTY;
        int[] sums = new int[3];
        int maxSum = 0;
        int count = 0;
        ItemArmor armor = null;
        for (int k = 0; k < inv.getSizeInventory(); ++k) {
            ItemStack stack = inv.getStackInSlot(k);
            if (stack.isEmpty()) continue;
            if (stack.getItem() instanceof ItemArmor a) {
                armor = a;
                if (a.getArmorMaterial() != ItemArmor.ArmorMaterial.LEATHER || !result.isEmpty()) return ItemStack.EMPTY;
                result = stack.copy();
                result.setCount(1);
                if (a.hasColor(stack)) {
                    int color = a.getColor(result);
                    float r = (color >> 16 & 255) / 255.0F;
                    float g = (color >> 8 & 255) / 255.0F;
                    float b = (color & 255) / 255.0F;
                    maxSum = (int) (maxSum + Math.max(r, Math.max(g, b)) * 255.0F);
                    sums[0] = (int) (sums[0] + r * 255.0F);
                    sums[1] = (int) (sums[1] + g * 255.0F);
                    sums[2] = (int) (sums[2] + b * 255.0F);
                    ++count;
                }
            } else {
                var dye = DyeUtils.colorFromStack(stack);
                if (dye.isEmpty()) return ItemStack.EMPTY;
                float[] c = dye.get().getColorComponentValues();
                int r = (int) (c[0] * 255.0F);
                int g = (int) (c[1] * 255.0F);
                int b = (int) (c[2] * 255.0F);
                maxSum += Math.max(r, Math.max(g, b));
                sums[0] += r;
                sums[1] += g;
                sums[2] += b;
                ++count;
            }
        }
        if (armor == null || count == 0) return ItemStack.EMPTY;
        int r = sums[0] / count;
        int g = sums[1] / count;
        int b = sums[2] / count;
        float avgMax = (float) maxSum / (float) count;
        float max = (float) Math.max(r, Math.max(g, b));
        r = (int) (r * avgMax / max);
        g = (int) (g * avgMax / max);
        b = (int) (b * avgMax / max);
        armor.setColor(result, (r << 8) + g << 8 | b);
        return result;
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
        return width * height >= 2;
    }
}
