package legacyconnector.v1_12_2.net.minecraft.inventory;

/** Told when an {@link InventoryBasic} changes. */
public interface IInventoryChangedListener {
    void onInventoryChanged(IInventory inventory);
}
