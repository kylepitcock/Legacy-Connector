package legacyconnector.v1_12_2.net.minecraftforge.fluids;

import legacyconnector.v1_12_2.net.minecraft.block.material.Material;

public class BlockFluidClassic extends BlockFluidBase {
    /** What draining a source block of this fluid gives (a bucket's worth by default). */
    protected FluidStack stack;

    public BlockFluidClassic(Fluid fluid, Material material) {
        super(fluid, material);
        this.stack = new FluidStack(fluid, 1000);
    }

    public BlockFluidClassic(Fluid fluid, Material material, legacyconnector.v1_12_2.net.minecraft.block.material.MapColor mapColor) {
        super(fluid, material, mapColor);
        this.stack = new FluidStack(fluid, 1000);
    }

    @Override
    public BlockFluidClassic setFluidStack(FluidStack stack) {
        this.stack = stack;
        return this;
    }

    public BlockFluidClassic setFluidStackAmount(int amount) {
        if (stack != null) this.stack = new FluidStack(stack.getFluid(), amount);
        return this;
    }

    public FluidStack mcc$stack() {
        return stack;
    }
}
