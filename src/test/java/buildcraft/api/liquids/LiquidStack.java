package buildcraft.api.liquids;

/** Test double of the BuildCraft 3.2 liquid stack Forestry 1.4.8 bundles (public itemID, amount, itemMeta). */
public class LiquidStack {
    public int itemID;
    public int amount;
    public int itemMeta;

    public LiquidStack(int itemID, int amount, int itemMeta) {
        this.itemID = itemID;
        this.amount = amount;
        this.itemMeta = itemMeta;
    }
}
