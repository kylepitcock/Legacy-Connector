package legacyconnector.v1_7_10.net.minecraft.item.crafting;

import java.util.ArrayList;
import java.util.List;
import legacyconnector.v1_7_10.net.minecraft.block.BlockColored;
import legacyconnector.v1_7_10.net.minecraft.entity.passive.EntitySheep;
import legacyconnector.v1_7_10.net.minecraft.init.Items;
import legacyconnector.v1_7_10.net.minecraft.inventory.InventoryCrafting;
import legacyconnector.v1_7_10.net.minecraft.item.ItemArmor;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/**
 * Dyeing leather armour, ported from 1.7.10: one piece of cloth armour plus dyes anywhere in the grid; the result's colour
 * is the average of the dyes (and of its old colour), kept as bright as the brightest of them.
 */
public class RecipesArmorDyes implements IRecipe {
    @Override
    public boolean matches(InventoryCrafting inv, World world) {
        ItemStack armor = null;
        List<ItemStack> dyes = new ArrayList<>();
        for (int i = 0; i < inv.getSizeInventory(); ++i) {
            ItemStack stack = inv.getStackInSlot(i);
            if (stack == null) continue;
            if (stack.getItem() instanceof ItemArmor item) {
                if (item.getArmorMaterial() != ItemArmor.ArmorMaterial.CLOTH || armor != null) return false;
                armor = stack;
            } else {
                if (stack.getItem() != Items.dye) return false;
                dyes.add(stack);
            }
        }
        return armor != null && !dyes.isEmpty();
    }

    @Override
    public ItemStack getCraftingResult(InventoryCrafting inv) {
        ItemStack result = null;
        int[] sum = new int[3];
        int brightness = 0;
        int count = 0;
        ItemArmor armor = null;
        for (int i = 0; i < inv.getSizeInventory(); ++i) {
            ItemStack stack = inv.getStackInSlot(i);
            if (stack == null) continue;
            if (stack.getItem() instanceof ItemArmor item) {
                armor = item;
                if (item.getArmorMaterial() != ItemArmor.ArmorMaterial.CLOTH || result != null) return null;
                result = stack.copy();
                result.mcc$setCount(1);
                if (item.hasColor(stack)) {
                    int color = item.getColor(result);
                    float r = (color >> 16 & 255) / 255.0F;
                    float g = (color >> 8 & 255) / 255.0F;
                    float b = (color & 255) / 255.0F;
                    brightness = (int) (brightness + Math.max(r, Math.max(g, b)) * 255.0F);
                    sum[0] = (int) (sum[0] + r * 255.0F);
                    sum[1] = (int) (sum[1] + g * 255.0F);
                    sum[2] = (int) (sum[2] + b * 255.0F);
                    ++count;
                }
            } else {
                if (stack.getItem() != Items.dye) return null;
                float[] fleece = EntitySheep.fleeceColorTable[BlockColored.func_150032_b(stack.getItemDamage())];
                int r = (int) (fleece[0] * 255.0F);
                int g = (int) (fleece[1] * 255.0F);
                int b = (int) (fleece[2] * 255.0F);
                brightness += Math.max(r, Math.max(g, b));
                sum[0] += r;
                sum[1] += g;
                sum[2] += b;
                ++count;
            }
        }
        if (armor == null || count == 0) return null;
        int r = sum[0] / count;
        int g = sum[1] / count;
        int b = sum[2] / count;
        float average = (float) brightness / count;
        float max = Math.max(r, Math.max(g, b));
        r = (int) (r * average / max);
        g = (int) (g * average / max);
        b = (int) (b * average / max);
        armor.func_82813_b(result, (r << 8) + g << 8 | b);
        return result;
    }

    @Override
    public int getRecipeSize() {
        return 10;
    }

    @Override
    public ItemStack getRecipeOutput() {
        return null;
    }
}
