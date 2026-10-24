package legacyconnector.v1_7_10.net.minecraftforge.event;

import legacyconnector.v1_7_10.cpw.mods.fml.common.eventhandler.Cancelable;
import legacyconnector.v1_7_10.cpw.mods.fml.common.eventhandler.Event;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;

/** The anvil is about to compute a result: handlers may supply their own output and costs. */
@Cancelable
public class AnvilUpdateEvent extends Event {
    public final ItemStack left;
    public final ItemStack right;
    public final String name;
    public ItemStack output;
    public int cost;
    public int materialCost;

    public AnvilUpdateEvent(ItemStack left, ItemStack right, String name, int cost) {
        this.left = left;
        this.right = right;
        this.name = name;
        this.cost = cost;
    }
}
