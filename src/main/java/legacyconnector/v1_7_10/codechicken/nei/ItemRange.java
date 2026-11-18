package legacyconnector.v1_7_10.codechicken.nei;

import java.util.ArrayList;
import java.util.List;
import legacyconnector.v1_6_4.net.minecraft.util.Ids;
import legacyconnector.v1_7_10.net.minecraft.item.Item;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;

/**
 * NEI 1.4.7 / 1.5.2's range of item ids (and damage values) of its item panel's subsets: {@code new ItemRange(id)}, {@code (id, firstDamage, lastDamage)},
 * {@code (firstId, lastId)}. The JEI bridge has no subsets; the range only expands to the stacks it names ({@link #stacks}) for
 * {@link MultiItemRange#add(java.util.Collection)}.
 */
public class ItemRange {
    public int firstID;
    public int firstDamage = -1;
    public int lastID;
    public int lastDamage = -1;
    public byte state;

    public ItemRange(int id) {
        this.firstID = id;
        this.lastID = id;
    }

    public ItemRange(int id, int firstDamage, int lastDamage) {
        this.firstID = id;
        this.lastID = id;
        this.firstDamage = firstDamage;
        this.lastDamage = lastDamage;
    }

    public ItemRange(int firstID, int lastID) {
        this.firstID = firstID;
        this.lastID = lastID;
    }

    public boolean isItemInRange(int id, int damage) {
        if (id < firstID || id > lastID) return false;
        if (firstDamage < 0) return true;
        return damage >= firstDamage && damage <= lastDamage;
    }

    /** The stacks the range names: every damage value of the range of each item with an id of it (damage -1 is the item's wildcard). */
    public List<ItemStack> stacks() {
        List<ItemStack> out = new ArrayList<>();
        for (int id = firstID; id <= lastID && id - firstID < 4096; id++) {
            Item item = Ids.item(id);
            if (item == null) continue;
            if (firstDamage < 0) {
                out.add(new ItemStack(item, 1, 32767));
            } else {
                for (int damage = firstDamage; damage <= lastDamage && damage - firstDamage < 4096; damage++) out.add(new ItemStack(item, 1, damage));
            }
        }
        return out;
    }

    @Override
    public String toString() {
        return firstID == lastID ? Integer.toString(firstID) : firstID + "-" + lastID;
    }
}
