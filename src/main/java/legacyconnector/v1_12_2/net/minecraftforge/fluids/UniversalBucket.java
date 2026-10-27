package legacyconnector.v1_12_2.net.minecraftforge.fluids;

import legacyconnector.v1_12_2.net.minecraft.item.Item;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;

/**
 * Forge's bucket for any fluid. The modern game has no universal bucket item; mods that make their own subclass get the container
 * rules (capacity, empty container); filled buckets of vanilla fluids are the vanilla buckets.
 */
public class UniversalBucket extends Item {
    private final int capacity;
    private final ItemStack empty;
    private final boolean nbtSensitive;

    public UniversalBucket() {
        this(Fluid.BUCKET_VOLUME, new ItemStack(legacyconnector.v1_12_2.net.minecraft.init.Items.BUCKET), false);
    }

    public UniversalBucket(int capacity, ItemStack empty, boolean nbtSensitive) {
        this.capacity = capacity;
        this.empty = empty;
        this.nbtSensitive = nbtSensitive;
        setMaxStackSize(1);
        setCreativeTab(legacyconnector.v1_12_2.net.minecraft.creativetab.CreativeTabs.MISC);
        setHasSubtypes(true);
    }

    /** A bucket filled with the fluid (vanilla's for water and lava, else empty: there is no universal bucket item to fill). */
    public static ItemStack getFilledBucket(UniversalBucket item, Fluid fluid) {
        return FluidUtil.getFilledBucket(new FluidStack(fluid, item == null ? Fluid.BUCKET_VOLUME : item.getCapacity()));
    }

    public FluidStack getFluid(ItemStack container) {
        return FluidUtil.getFluidContained(container);
    }

    public int getCapacity() {
        return capacity;
    }

    public ItemStack getEmpty() {
        return empty;
    }

    public boolean isNbtSensitive() {
        return nbtSensitive;
    }
}
