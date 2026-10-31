package legacyconnector.v1_7_10.net.minecraft.util;

import java.util.Random;
import legacyconnector.v1_7_10.net.minecraft.inventory.IInventory;
import legacyconnector.v1_7_10.net.minecraft.item.Item;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;

/** A weighted loot entry: item, stack size range, weight. */
public class WeightedRandomChestContent extends WeightedRandom.Item {
    public ItemStack theItemId;
    public int theMinimumChanceToGenerateItem;
    public int theMaximumChanceToGenerateItem;

    public WeightedRandomChestContent(Item item, int meta, int min, int max, int weight) {
        this(new ItemStack(item, 1, meta), min, max, weight);
    }

    public WeightedRandomChestContent(ItemStack stack, int min, int max, int weight) {
        super(weight);
        this.theItemId = stack;
        this.theMinimumChanceToGenerateItem = min;
        this.theMaximumChanceToGenerateItem = max;
    }

    /** Forge: the stacks this entry contributes to one chest. */
    public ItemStack[] generateChestContent(Random random, IInventory inventory) {
        int count = theMinimumChanceToGenerateItem
                + (theMaximumChanceToGenerateItem > theMinimumChanceToGenerateItem ? random.nextInt(theMaximumChanceToGenerateItem - theMinimumChanceToGenerateItem + 1) : 0);
        ItemStack stack = theItemId.copy();
        ItemStack.mcc$set$stackSize(stack, count);
        return new ItemStack[] {stack};
    }

    public static void generateChestContents(Random random, WeightedRandomChestContent[] items, IInventory inventory, int rolls) {
        // A chest filled from one of Forge's categories (dungeon, mineshaft, temple, stronghold...) gets the matching modern loot
        // table instead: the pack's loot and modern mods' additions apply when it is opened, and the legacy mods' additions are
        // in those tables too (ChestLootBridge1710). Filled here, it held 1.7.10's vanilla items and legacy mod items only.
        var table = org.kylepitcock.legacyconnector.v1_7_10.bridge.ChestLootBridge1710.tableFor(
                legacyconnector.v1_7_10.net.minecraftforge.common.ChestGenHooks.mcc$categoryOf(items));
        if (table != null && inventory instanceof legacyconnector.v1_7_10.net.minecraft.tileentity.TileEntityChest chest
                && chest.mcc$container() instanceof net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity r) {
            r.setLootTable(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE, table), random.nextLong());
            return;
        }
        for (int i = 0; i < rolls && items.length > 0; i++) {
            WeightedRandomChestContent entry = (WeightedRandomChestContent) WeightedRandom.getRandomItem(random, items);
            for (ItemStack s : entry.generateChestContent(random, inventory)) {
                inventory.setInventorySlotContents(random.nextInt(inventory.getSizeInventory()), s);
            }
        }
    }

    public static WeightedRandomChestContent[] func_92080_a(WeightedRandomChestContent[] a, WeightedRandomChestContent... b) {
        WeightedRandomChestContent[] out = java.util.Arrays.copyOf(a, a.length + b.length);
        System.arraycopy(b, 0, out, a.length, b.length);
        return out;
    }
}
