package legacyconnector.v1_12_2.net.minecraft.item;

/**
 * 1.12.2 soup: food in a bowl, one per stack; eating it leaves the bowl (the modern food component's "converts to", so the eating itself
 * stays the game's).
 */
public class ItemSoup extends ItemFood {
    public ItemSoup(int healAmount) {
        super(healAmount, false);
        setMaxStackSize(1);
    }
}
