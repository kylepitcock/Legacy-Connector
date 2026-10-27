package legacyconnector.v1_12_2.net.minecraftforge.event;

import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Cancelable;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Event;

/**
 * Both anvil inputs changed: a handler that sets an output makes it the result (with its cost); canceling leaves no result.
 * Fed from the modern anvil event (see {@code EventBridge1122}).
 */
@Cancelable
public class AnvilUpdateEvent extends Event {
    private final ItemStack left;
    private final ItemStack right;
    private final String name;
    private ItemStack output;
    private int cost;
    private int materialCost;

    public AnvilUpdateEvent(ItemStack left, ItemStack right, String name, int cost) {
        this.left = left;
        this.right = right;
        this.output = ItemStack.EMPTY;
        this.name = name;
        this.setCost(cost);
        this.setMaterialCost(0);
    }

    public ItemStack getLeft() {
        return left;
    }

    public ItemStack getRight() {
        return right;
    }

    public String getName() {
        return name;
    }

    public ItemStack getOutput() {
        return output;
    }

    public void setOutput(ItemStack output) {
        this.output = output;
    }

    public int getCost() {
        return cost;
    }

    public void setCost(int cost) {
        this.cost = cost;
    }

    public int getMaterialCost() {
        return materialCost;
    }

    public void setMaterialCost(int materialCost) {
        this.materialCost = materialCost;
    }
}
