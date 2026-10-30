package legacyconnector.v1_12_2.net.minecraftforge.client;

import legacyconnector.v1_12_2.net.minecraft.client.renderer.color.IItemColor;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraftforge.fluids.FluidStack;
import legacyconnector.v1_12_2.net.minecraftforge.fluids.FluidUtil;

/** Forge: tints tint index 1 of a fluid container with the colour of the fluid it holds (white when empty). */
public class FluidContainerColorer implements IItemColor {
    @Override
    public int colorMultiplier(ItemStack stack, int tintIndex) {
        if (tintIndex != 1) return 0xFFFFFFFF;
        FluidStack fluidStack = FluidUtil.getFluidContained(stack);
        if (fluidStack == null || fluidStack.getFluid() == null) return 0xFFFFFFFF;
        return fluidStack.getFluid().getColor(fluidStack);
    }
}
