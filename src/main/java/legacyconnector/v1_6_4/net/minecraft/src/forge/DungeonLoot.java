package legacyconnector.v1_6_4.net.minecraft.src.forge;

public class DungeonLoot {
    private legacyconnector.v1_7_10.net.minecraft.item.ItemStack itemStack;
    private int minCount = 1;
    private int maxCount = 1;

    public DungeonLoot(legacyconnector.v1_7_10.net.minecraft.item.ItemStack stack, int min, int max) {
        this.itemStack = stack;
        this.minCount = min;
        this.maxCount = max;
    }

    public legacyconnector.v1_7_10.net.minecraft.item.ItemStack generateStack(java.util.Random rand) {
        legacyconnector.v1_7_10.net.minecraft.item.ItemStack ret = itemStack.copy();
        legacyconnector.v1_7_10.net.minecraft.item.ItemStack.mcc$set$stackSize(ret, minCount + (maxCount > minCount ? rand.nextInt(maxCount - minCount + 1) : 0));
        return ret;
    }

    public boolean equals(legacyconnector.v1_7_10.net.minecraft.item.ItemStack stack, int min, int max) {
        return equals(stack) && min == minCount && max == maxCount;
    }

    public boolean equals(legacyconnector.v1_7_10.net.minecraft.item.ItemStack stack) {
        return stack.isItemEqual(itemStack) && ItemStackTags.same(stack, itemStack);
    }
}
