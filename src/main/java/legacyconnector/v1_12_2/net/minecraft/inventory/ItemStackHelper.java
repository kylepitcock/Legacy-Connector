package legacyconnector.v1_12_2.net.minecraft.inventory;

import java.util.List;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagList;
import legacyconnector.v1_12_2.net.minecraft.util.NonNullList;

/** Inventory list helpers: splitting a slot and saving/loading stacks as an {@code Items} list of {@code Slot}-indexed compounds. */
public class ItemStackHelper {
    public static ItemStack getAndSplit(List<ItemStack> stacks, int index, int amount) {
        return index >= 0 && index < stacks.size() && !stacks.get(index).isEmpty() && amount > 0 ? stacks.get(index).splitStack(amount) : ItemStack.EMPTY;
    }

    public static ItemStack getAndRemove(List<ItemStack> stacks, int index) {
        if (index < 0 || index >= stacks.size()) return ItemStack.EMPTY;
        ItemStack s = stacks.get(index);
        stacks.set(index, ItemStack.EMPTY);
        return s;
    }

    public static NBTTagCompound saveAllItems(NBTTagCompound tag, NonNullList<ItemStack> list) {
        return saveAllItems(tag, list, true);
    }

    public static NBTTagCompound saveAllItems(NBTTagCompound tag, NonNullList<ItemStack> list, boolean saveEmpty) {
        NBTTagList items = new NBTTagList();
        for (int i = 0; i < list.size(); i++) {
            ItemStack s = list.get(i);
            if (!s.isEmpty()) {
                NBTTagCompound c = new NBTTagCompound();
                c.setByte("Slot", (byte) i);
                s.writeToNBT(c);
                items.appendTag(c);
            }
        }
        if (items.tagCount() > 0 || saveEmpty) tag.setTag("Items", items);
        return tag;
    }

    public static void loadAllItems(NBTTagCompound tag, NonNullList<ItemStack> list) {
        NBTTagList items = tag.getTagList("Items", 10);
        for (int i = 0; i < items.tagCount(); i++) {
            NBTTagCompound c = items.getCompoundTagAt(i);
            int j = c.getByte("Slot") & 255;
            if (j < list.size()) list.set(j, new ItemStack(c));
        }
    }
}
