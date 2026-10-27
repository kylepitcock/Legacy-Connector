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

    /** The fluid level of the block (0 is a source block), stored as its metadata. */
    public static final legacyconnector.v1_12_2.net.minecraft.block.properties.PropertyInteger LEVEL =
            legacyconnector.v1_12_2.net.minecraft.block.properties.PropertyInteger.create("level", 0, 15);

    @Override
    protected legacyconnector.v1_12_2.net.minecraft.block.state.BlockStateContainer createBlockState() {
        return new legacyconnector.v1_12_2.net.minecraft.block.state.BlockStateContainer(this, LEVEL);
    }

    @Override
    public legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState getStateFromMeta(int meta) {
        return getDefaultState().withProperty(LEVEL, meta & 15);
    }

    @Override
    public int getMetaFromState(legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state) {
        return state.getValue(LEVEL);
    }

    public int getQuantaValue(legacyconnector.v1_12_2.net.minecraft.world.IBlockAccess world, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos) {
        var state = world.getBlockState(pos);
        if (state.getBlock() == legacyconnector.v1_12_2.net.minecraft.init.Blocks.AIR) return 0;
        if (state.getBlock() != this) return -1;
        return quantaPerBlock - state.getValue(LEVEL);
    }

    public int getMaxRenderHeightMeta() {
        return quantaPerBlock - 1;
    }

    public int getQuantaPerBlock() {
        return quantaPerBlock;
    }

    public int getDensityDir() {
        return densityDir;
    }

    /** Fluids are drawn by the modern fluid renderer, not as models. */
    @Override
    public legacyconnector.v1_12_2.net.minecraft.util.EnumBlockRenderType getRenderType(legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state) {
        return legacyconnector.v1_12_2.net.minecraft.util.EnumBlockRenderType.MODEL;
    }

    @Override
    public boolean isOpaqueCube(legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state) {
        return false;
    }

    @Override
    public boolean isFullCube(legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state) {
        return false;
    }

    @Override
    public legacyconnector.v1_12_2.net.minecraft.util.math.AxisAlignedBB getCollisionBoundingBox(legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state,
            legacyconnector.v1_12_2.net.minecraft.world.IBlockAccess world, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos) {
        return NULL_AABB;
    }

    @Override
    public boolean isReplaceable(legacyconnector.v1_12_2.net.minecraft.world.IBlockAccess world, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos) {
        return true;
    }
}
