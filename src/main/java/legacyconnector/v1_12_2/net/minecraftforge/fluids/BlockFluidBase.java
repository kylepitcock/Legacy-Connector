package legacyconnector.v1_12_2.net.minecraftforge.fluids;

import legacyconnector.v1_12_2.net.minecraft.block.Block;
import legacyconnector.v1_12_2.net.minecraft.block.material.Material;

/** The blocks of placed fluids; the world behaviour is the modern fluid's. */
public abstract class BlockFluidBase extends Block implements IFluidBlock {
    protected final String fluidName;
    protected int quantaPerBlock = 8;
    protected int densityDir = -1;
    protected boolean canDisplace = true;
    protected int tickRate = 5;

    public BlockFluidBase(Fluid fluid, Material material) {
        super(material);
        this.fluidName = fluid.getName();
    }

    @Override
    public Fluid getFluid() {
        return FluidRegistry.getFluid(fluidName);
    }

    public BlockFluidBase setQuantaPerBlock(int quanta) {
        this.quantaPerBlock = quanta;
        return this;
    }

    public BlockFluidBase setTickRate(int tickRate) {
        this.tickRate = tickRate;
        return this;
    }

    public BlockFluidBase setDensity(int density) {
        return this;
    }

    public BlockFluidBase setFluidStack(FluidStack stack) {
        return this;
    }
}
