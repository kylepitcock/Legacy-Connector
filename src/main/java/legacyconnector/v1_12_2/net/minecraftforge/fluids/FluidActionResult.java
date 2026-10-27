package legacyconnector.v1_12_2.net.minecraftforge.fluids;

import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;

/** The result of filling or emptying a container: whether it worked and the resulting container stack. */
public class FluidActionResult {
    public static final FluidActionResult FAILURE = new FluidActionResult(false, ItemStack.EMPTY);

    public final boolean success;
    public final ItemStack result;

    public FluidActionResult(ItemStack result) {
        this(true, result);
    }

    public FluidActionResult(boolean success, ItemStack result) {
        this.success = success;
        this.result = result;
    }

    public boolean isSuccess() {
        return success;
    }

    public ItemStack getResult() {
        return result;
    }
}
