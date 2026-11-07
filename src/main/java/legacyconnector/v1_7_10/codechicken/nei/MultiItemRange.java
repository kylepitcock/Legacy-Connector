package legacyconnector.v1_7_10.codechicken.nei;

import java.util.ArrayList;
import java.util.List;
import legacyconnector.v1_7_10.net.minecraft.block.Block;
import legacyconnector.v1_7_10.net.minecraft.item.Item;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;

/**
 * NEI's list of item ranges (a mod's items grouped under a subset of NEI's item panel). The bridge to JEI has no item-panel subsets, so the ranges
 * are only kept: mods fill them while configuring NEI and hand them to {@link legacyconnector.v1_7_10.codechicken.nei.api.API#addSetRange}.
 */
public class MultiItemRange {
    private final List<ItemStack> stacks = new ArrayList<>();

    public MultiItemRange() {
    }

    public MultiItemRange add(Item item) {
        if (item != null) stacks.add(new ItemStack(item, 1, 32767));
        return this;
    }

    public MultiItemRange add(Block block) {
        if (block != null) stacks.add(new ItemStack(block, 1, 32767));
        return this;
    }

    public MultiItemRange add(ItemStack stack) {
        if (stack != null) stacks.add(stack);
        return this;
    }

    public MultiItemRange add(MultiItemRange other) {
        if (other != null) stacks.addAll(other.stacks);
        return this;
    }

    public List<ItemStack> mcc$stacks() {
        return stacks;
    }
}
