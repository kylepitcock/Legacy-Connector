package legacyconnector.v1_7_10.codechicken.nei.api;

import java.util.ArrayList;
import java.util.List;
import legacyconnector.v1_7_10.net.minecraft.inventory.IInventory;
import legacyconnector.v1_7_10.net.minecraft.inventory.Slot;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;

/** A named inventory area of a GUI (NEI's fast-transfer targets). */
public class TaggedInventoryArea {
    public final String tagName;
    public final List<Slot> slots = new ArrayList<>();
    public final IInventory inventory;

    public TaggedInventoryArea(IInventory inv) {
        this("", inv);
    }

    public TaggedInventoryArea(String tag, IInventory inv) {
        this.tagName = tag;
        this.inventory = inv;
    }

    public List<ItemStack> contents() {
        List<ItemStack> list = new ArrayList<>();
        for (int i = 0; i < inventory.getSizeInventory(); i++) list.add(inventory.getStackInSlot(i));
        return list;
    }
}
