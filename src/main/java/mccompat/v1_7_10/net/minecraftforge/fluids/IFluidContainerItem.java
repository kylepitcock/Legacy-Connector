package mccompat.v1_7_10.net.minecraftforge.fluids;

import mccompat.v1_7_10.net.minecraft.item.ItemStack;

public interface IFluidContainerItem {
    FluidStack getFluid(ItemStack container);

    int getCapacity(ItemStack container);

    int fill(ItemStack container, FluidStack resource, boolean doFill);

    FluidStack drain(ItemStack container, int maxDrain, boolean doDrain);
}
