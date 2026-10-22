package legacyconnector.v1_7_10.codechicken.nei;

import legacyconnector.v1_7_10.net.minecraft.item.Item;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;
import legacyconnector.v1_7_10.net.minecraftforge.oredict.OreDictionary;

/** An item slot in an NEI recipe: position (relative to the handler's background) and the stacks it cycles through. */
public class PositionedStack {
    public int relx;
    public int rely;
    public ItemStack[] items;
    public ItemStack item;

    public PositionedStack(Object object, int x, int y, boolean genPerms) {
        this.items = NEIServerUtils.extractRecipeItems(object);
        this.relx = x;
        this.rely = y;
        if (genPerms) generatePermutations();
        else setPermutationToRender(0);
    }

    public PositionedStack(Object object, int x, int y) {
        this(object, x, y, true);
    }

    /** Wildcard damage values become concrete (damage 0); NEI expanded them to every subtype. */
    public void generatePermutations() {
        for (int i = 0; i < items.length; i++) {
            if (items[i] != null && items[i].getItemDamage() == OreDictionary.WILDCARD_VALUE) {
                ItemStack s = items[i].copy();
                s.setItemDamage(0);
                items[i] = s;
            }
        }
        setPermutationToRender(0);
    }

    public void setMaxSize(int size) {
        for (ItemStack s : items) {
            if (s != null && ItemStack.mcc$get$stackSize(s) > size) ItemStack.mcc$set$stackSize(s, size);
        }
    }

    public PositionedStack copy() {
        PositionedStack c = new PositionedStack(items, relx, rely, false);
        c.item = item == null ? null : item.copy();
        return c;
    }

    public void setPermutationToRender(int index) {
        if (items.length == 0) {
            item = null;
            return;
        }
        item = items[Math.floorMod(index, items.length)].copy();
        if (item.getItemDamage() == OreDictionary.WILDCARD_VALUE) item.setItemDamage(0);
    }

    public boolean contains(ItemStack stack) {
        for (ItemStack s : items) if (NEIServerUtils.areStacksSameTypeCrafting(s, stack)) return true;
        return false;
    }

    public boolean contains(Item item) {
        for (ItemStack s : items) if (s != null && s.getItem() == item) return true;
        return false;
    }
}
