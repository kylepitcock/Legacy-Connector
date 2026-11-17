package buildcraft.api;

/** Test double of BuildCraft 3.1's interface, which ships inside the mods (found by name by the layer). */
public interface ISpecialInventory {
    boolean addItem(legacyconnector.v1_7_10.net.minecraft.item.ItemStack stack, boolean doAdd, Orientations from);

    legacyconnector.v1_7_10.net.minecraft.item.ItemStack extractItem(boolean doRemove, Orientations from);
}
