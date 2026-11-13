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

    /**
     * A bucket filled with the fluid: Forge's universal bucket gives {@link FluidUtil#getFilledBucket} (vanilla's buckets for water and lava,
     * the fluid's modern bucket otherwise); a mod's own universal bucket keeps the fluid in its tag, as Forge did.
     */
    public static ItemStack getFilledBucket(UniversalBucket item, Fluid fluid) {
        if (item == null || item == legacyconnector.v1_12_2.net.minecraftforge.common.ForgeModContainer.mcc$get$universalBucket(null) || fluid == null
                || fluid == FluidRegistry.WATER || fluid == FluidRegistry.LAVA) {
            return FluidUtil.getFilledBucket(new FluidStack(fluid, item == null ? Fluid.BUCKET_VOLUME : item.getCapacity()));
        }
        ItemStack bucket = new ItemStack(item);
        var tag = new legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound();
        new FluidStack(fluid, item.getCapacity()).writeToNBT(tag);
        bucket.setTagCompound(tag);
        return bucket;
    }

    /** The fluid in the bucket's tag (Forge's format), or null. */
    public FluidStack getFluid(ItemStack container) {
        return container == null ? null : FluidStack.loadFluidStackFromNBT(container.getTagCompound());
    }

    @Override
    public legacyconnector.v1_12_2.net.minecraftforge.common.capabilities.ICapabilityProvider initCapabilities(ItemStack stack,
            legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound nbt) {
        return new legacyconnector.v1_12_2.net.minecraftforge.fluids.capability.wrappers.FluidBucketWrapper(stack);
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
