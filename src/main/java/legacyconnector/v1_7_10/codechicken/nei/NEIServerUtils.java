package legacyconnector.v1_7_10.codechicken.nei;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import legacyconnector.v1_7_10.net.minecraft.block.Block;
import legacyconnector.v1_7_10.net.minecraft.item.Item;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;
import legacyconnector.v1_7_10.net.minecraftforge.oredict.OreDictionary;

/** NEI's stack helpers (the ones mods call). */
public class NEIServerUtils {
    public static boolean areStacksSameType(ItemStack a, ItemStack b) {
        return a != null && b != null && a.getItem() == b.getItem()
                && (!a.getHasSubtypes() || a.getItemDamage() == b.getItemDamage()) && ItemStack.areItemStackTagsEqual(b, a);
    }

    public static boolean areStacksSameTypeCrafting(ItemStack a, ItemStack b) {
        return a != null && b != null && a.getItem() == b.getItem()
                && (a.getItemDamage() == b.getItemDamage() || a.getItemDamage() == OreDictionary.WILDCARD_VALUE
                || b.getItemDamage() == OreDictionary.WILDCARD_VALUE || a.getItem().isDamageable());
    }

    public static boolean areStacksSameTypeWithNBT(ItemStack a, ItemStack b) {
        return areStacksSameType(a, b);
    }

    public static boolean areStacksIdentical(ItemStack a, ItemStack b) {
        return a == null || b == null ? a == b : areStacksSameType(a, b) && ItemStack.mcc$get$stackSize(a) == ItemStack.mcc$get$stackSize(b);
    }

    public static ItemStack copyStack(ItemStack stack, int quantity) {
        if (stack == null) return null;
        ItemStack copy = stack.copy();
        ItemStack.mcc$set$stackSize(copy, quantity);
        return copy;
    }

    public static ItemStack copyStack(ItemStack stack) {
        return stack == null ? null : stack.copy();
    }

    /** Ingredient object (stack, item, block, ore name, array or list of those) -> the stacks it stands for. */
    public static ItemStack[] extractRecipeItems(Object in) {
        List<ItemStack> out = new ArrayList<>();
        add(in, out);
        return out.toArray(new ItemStack[0]);
    }

    private static void add(Object in, List<ItemStack> out) {
        if (in instanceof ItemStack s) out.add(s.copy());
        else if (in instanceof Item i) out.add(new ItemStack(i));
        else if (in instanceof Block b) out.add(new ItemStack(b));
        else if (in instanceof String ore) for (ItemStack s : OreDictionary.getOres(ore)) out.add(s.copy());
        else if (in instanceof Object[] arr) for (Object o : arr) add(o, out);
        else if (in instanceof Collection<?> c) for (Object o : c) add(o, out);
    }
}
