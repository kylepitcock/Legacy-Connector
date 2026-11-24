package legacyconnector.v1_12_2.mezz.jei.network.packets;

import java.util.List;
import java.util.Map;

/** JEI 4 (1.12.2) internal: the "move items into the recipe grid" request a recipe transfer handler sends. */
public class PacketRecipeTransfer extends PacketJei {
    public final Map<Integer, Integer> recipeMap;
    public final List<Integer> craftingSlots;
    public final List<Integer> inventorySlots;
    public final boolean maxTransfer;
    public final boolean requireCompleteSets;

    public PacketRecipeTransfer(Map<Integer, Integer> recipeMap, List<Integer> craftingSlots, List<Integer> inventorySlots, boolean maxTransfer, boolean requireCompleteSets) {
        this.recipeMap = recipeMap;
        this.craftingSlots = craftingSlots;
        this.inventorySlots = inventorySlots;
        this.maxTransfer = maxTransfer;
        this.requireCompleteSets = requireCompleteSets;
    }
}
