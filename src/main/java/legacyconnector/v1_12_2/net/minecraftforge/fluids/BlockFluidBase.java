package legacyconnector.v1_12_2.net.minecraftforge.fluids;

import legacyconnector.v1_12_2.net.minecraft.block.Block;
import legacyconnector.v1_12_2.net.minecraft.block.material.Material;

/** The blocks of placed fluids; the world behaviour is the modern fluid's. */
public abstract class BlockFluidBase extends Block implements IFluidBlock {
    /** The unlisted render properties of 1.12's fluid blocks (the layer renders fluids itself, so none are listed). */
    public static final com.google.common.collect.ImmutableList<legacyconnector.v1_12_2.net.minecraftforge.common.property.IUnlistedProperty<Float>> FLUID_RENDER_PROPS =
            com.google.common.collect.ImmutableList.of();

    protected final String fluidName;
    protected int quantaPerBlock = 8;
    protected int densityDir = -1;
    protected boolean canDisplace = true;
    protected int tickRate = 5;

    public BlockFluidBase(Fluid fluid, Material material, legacyconnector.v1_12_2.net.minecraft.block.material.MapColor mapColor) {
        super(material, mapColor);
        this.fluidName = fluid.getName();
        if (fluid.getBlock() == null) fluid.setBlock(this);
    }

    public BlockFluidBase(Fluid fluid, Material material) {
        super(material);
        this.fluidName = fluid.getName();
        if (fluid.getBlock() == null) fluid.setBlock(this);
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
