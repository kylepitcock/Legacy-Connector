package legacyconnector.v1_7_10.codechicken.nei.recipe;

import java.util.ArrayList;
import java.util.List;
import legacyconnector.v1_7_10.codechicken.nei.api.IOverlayHandler;
import legacyconnector.v1_7_10.net.minecraft.client.gui.inventory.GuiContainer;
import legacyconnector.v1_7_10.net.minecraft.inventory.Slot;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;

/** NEI's recipe-to-crafting-grid transfer; JEI does its own transfers, so the overlay itself does nothing. */
public class DefaultOverlayHandler implements IOverlayHandler {
    /** One ingredient of the recipe being moved and how many of it the inventory holds. */
    public static class DistributedIngred {
        public ItemStack stack;
        public int invAmount;
        public int distributed;
        public int numSlots;
        public int recipeAmount;

        public DistributedIngred(ItemStack item) {
            stack = item.copy();
        }
    }

    /** One permutation of an ingredient and the crafting slots it fills. */
    public static class IngredientDistribution {
        public DistributedIngred distrib;
        public ItemStack permutation;
        public Slot[] slots;

        public IngredientDistribution(DistributedIngred distrib, ItemStack permutation) {
            this.distrib = distrib;
            this.permutation = permutation;
        }
    }

    public final int offsetx;
    public final int offsety;

    public DefaultOverlayHandler() {
        this(5, 11);
    }

    public DefaultOverlayHandler(int x, int y) {
        this.offsetx = x;
        this.offsety = y;
    }

    /** Whether items may be taken out of the slot to fill the grid: any filled slot that is not an output. */
    public boolean canMoveFrom(Slot slot, GuiContainer gui) {
        return slot.getHasStack();
    }

    /**
     * NEI: for each ingredient of the recipe the slots of the screen's container that sit where the ingredient is drawn (the recipe's origin
     * plus this handler's offset), so the ingredient can be moved there. There is no NEI transfer under JEI; handlers of 1.4.7 / 1.5.2 call it
     * from their own overlay code.
     */
    public Slot[][] mapIngredSlots(GuiContainer gui, List<legacyconnector.v1_7_10.codechicken.nei.PositionedStack> ingredients) {
        Slot[][] map = new Slot[ingredients.size()][];
        for (int i = 0; i < map.length; i++) {
            legacyconnector.v1_7_10.codechicken.nei.PositionedStack stack = ingredients.get(i);
            List<Slot> found = new ArrayList<>();
            if (gui != null && gui.inventorySlots != null) {
                for (Object o : gui.inventorySlots.inventorySlots) {
                    Slot slot = (Slot) o;
                    if (slot.xDisplayPosition == stack.relx + offsetx && slot.yDisplayPosition == stack.rely + offsety) found.add(slot);
                }
            }
            map[i] = found.toArray(new Slot[0]);
        }
        return map;
    }

    /** The entry of the list that holds the same item as the stack, or null. */
    public DistributedIngred findIngred(List<DistributedIngred> ingredients, ItemStack stack) {
        for (DistributedIngred ingred : ingredients) {
            if (legacyconnector.v1_7_10.codechicken.nei.NEIServerUtils.areStacksSameTypeCrafting(ingred.stack, stack)) return ingred;
        }
        return null;
    }

    public List<DistributedIngred> ingredientsOf(List<ItemStack> stacks) {
        List<DistributedIngred> list = new ArrayList<>();
        for (ItemStack s : stacks) if (findIngred(list, s) == null) list.add(new DistributedIngred(s));
        return list;
    }
}
