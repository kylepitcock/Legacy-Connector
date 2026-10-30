package legacyconnector.v1_12_2.net.minecraftforge.fluids;

import java.util.HashMap;
import java.util.Map;
import legacyconnector.v1_12_2.net.minecraft.block.Block;
import legacyconnector.v1_12_2.net.minecraft.block.material.Material;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.util.BlockRenderLayer;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.util.math.Vec3d;
import legacyconnector.v1_12_2.net.minecraft.world.IBlockAccess;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/**
 * Forge 1.12.2's base of fluid blocks: the fluid's quanta per block (the level is the block's {@link #LEVEL}), its density (which way it
 * flows and what it displaces), tick rate, temperature and render layer, and the displacement rules every fluid block shares.
 */
public abstract class BlockFluidBase extends Block implements IFluidBlock {
    /** The unlisted render properties of 1.12's fluid blocks (the layer renders fluids itself, so none are listed). */
    public static final com.google.common.collect.ImmutableList<legacyconnector.v1_12_2.net.minecraftforge.common.property.IUnlistedProperty<Float>> FLUID_RENDER_PROPS =
            com.google.common.collect.ImmutableList.of();
    /** The fluid level of the block (0 is a source block), stored as its metadata. */
    public static final legacyconnector.v1_12_2.net.minecraft.block.properties.PropertyInteger LEVEL =
            legacyconnector.v1_12_2.net.minecraft.block.properties.PropertyInteger.create("level", 0, 15);
    /** Forge's unlisted render data of a fluid block: the flow angle, the heights of the four top corners and the side overlays. */
    public static final legacyconnector.v1_12_2.net.minecraftforge.common.property.PropertyFloat FLOW_DIRECTION =
            new legacyconnector.v1_12_2.net.minecraftforge.common.property.PropertyFloat("flow_direction", -1000.0F, 1000.0F);
    public static final legacyconnector.v1_12_2.net.minecraftforge.common.property.PropertyFloat[] LEVEL_CORNERS = new legacyconnector.v1_12_2.net.minecraftforge.common.property.PropertyFloat[4];
    public static final UnlistedPropertyBool[] SIDE_OVERLAYS = new UnlistedPropertyBool[4];

    static {
        for (int i = 0; i < 4; i++) {
            LEVEL_CORNERS[i] = new legacyconnector.v1_12_2.net.minecraftforge.common.property.PropertyFloat("level_corner_" + i, 0.0F, 1.0F);
            SIDE_OVERLAYS[i] = new UnlistedPropertyBool("side_overlay_" + i);
        }
    }

    /** An unlisted boolean property (a listed one used as unlisted). */
    public static final class UnlistedPropertyBool extends legacyconnector.v1_12_2.net.minecraftforge.common.property.Properties.PropertyAdapter<Boolean> {
        public UnlistedPropertyBool(String name) {
            super(legacyconnector.v1_12_2.net.minecraft.block.properties.PropertyBool.create(name));
        }
    }

    /** Blocks every fluid washes away (true) or leaves alone (false), whatever their material says. */
    protected static final Map<Block, Boolean> defaultDisplacements = new HashMap<>();

    protected Map<Block, Boolean> displacements = new HashMap<>(defaultDisplacements);
    protected int quantaPerBlock = 8;
    protected float quantaPerBlockFloat = 8.0F;
    protected float quantaFraction = 8.0F / 9.0F;
    protected int density = 1;
    protected int densityDir = -1;
    protected int temperature = 295;
    protected int tickRate = 1;
    protected BlockRenderLayer renderLayer = BlockRenderLayer.TRANSLUCENT;
    protected int maxScaledLight = 0;
    protected boolean canDisplace = true;
    protected final String fluidName;
    protected final Fluid definedFluid;

    public BlockFluidBase(Fluid fluid, Material material, legacyconnector.v1_12_2.net.minecraft.block.material.MapColor mapColor) {
        super(material, mapColor);
        this.fluidName = fluid.getName();
        this.definedFluid = fluid;
        mcc$init(fluid);
    }

    public BlockFluidBase(Fluid fluid, Material material) {
        super(material);
        this.fluidName = fluid.getName();
        this.definedFluid = fluid;
        mcc$init(fluid);
    }

    private void mcc$init(Fluid fluid) {
        setTickRate(20);
        setDensity(fluid.getDensity());
        this.temperature = fluid.getTemperature();
        this.maxScaledLight = fluid.getLuminosity();
        if (fluid.getBlock() == null) fluid.setBlock(this);
    }

    @Override
    public Fluid getFluid() {
        Fluid f = FluidRegistry.getFluid(fluidName);
        return f != null ? f : definedFluid;
    }

    public BlockFluidBase setQuantaPerBlock(int quantaPerBlock) {
        if (quantaPerBlock > 16 || quantaPerBlock < 1) quantaPerBlock = 8;
        this.quantaPerBlock = quantaPerBlock;
        this.quantaPerBlockFloat = quantaPerBlock;
        this.quantaFraction = (quantaPerBlock - 1) / (float) quantaPerBlock;
        return this;
    }

    public BlockFluidBase setRenderLayer(BlockRenderLayer renderLayer) {
        this.renderLayer = renderLayer;
        return this;
    }

    public BlockFluidBase setTickRate(int tickRate) {
        if (tickRate <= 0) tickRate = 20;
        this.tickRate = tickRate;
        return this;
    }

    public BlockFluidBase setMaxScaledLight(int maxScaledLight) {
        this.maxScaledLight = maxScaledLight;
        return this;
    }

    public BlockFluidBase setDensity(int density) {
        if (density == 0) density = 1;
        this.density = density;
        this.densityDir = density > 0 ? -1 : 1;
        return this;
    }

    public BlockFluidBase setTemperature(int temperature) {
        this.temperature = temperature;
        return this;
    }

    public BlockFluidBase setFluidStack(FluidStack stack) {
        return this;
    }

    @Override
    protected legacyconnector.v1_12_2.net.minecraft.block.state.BlockStateContainer createBlockState() {
        return new legacyconnector.v1_12_2.net.minecraft.block.state.BlockStateContainer(this, LEVEL);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState().withProperty(LEVEL, meta & 15);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(LEVEL);
    }

    // ------------------------------------------------------------------ displacement

    /** Whether the fluid could flow into the position (air, blocks its displacement rules or materials let it wash away, lighter fluids). */
    public boolean canDisplace(IBlockAccess world, BlockPos pos) {
        if (world.isAirBlock(pos)) return true;
        IBlockState state = world.getBlockState(pos);
        Block block = state.getBlock();
        if (block == this) return false;
        if (displacements.containsKey(block)) return displacements.get(block);
        Material material = state.getMaterial();
        if (material.blocksMovement() || material == Material.PORTAL || material == Material.STRUCTURE_VOID) return false;
        int otherDensity = getDensity(world, pos);
        if (otherDensity == Integer.MAX_VALUE) return true;
        return this.density > otherDensity;
    }

    /** Like {@link #canDisplace}, dropping what is washed away; true when the fluid may flow in. */
    public boolean displaceIfPossible(World world, BlockPos pos) {
        if (world.isAirBlock(pos)) return true;
        IBlockState state = world.getBlockState(pos);
        Block block = state.getBlock();
        if (block == this) return false;
        if (displacements.containsKey(block)) {
            if (displacements.get(block)) {
                if (block != legacyconnector.v1_12_2.net.minecraft.init.Blocks.SNOW_LAYER) block.dropBlockAsItemWithChance(world, pos, state, 1.0F, 0);
                return true;
            }
            return false;
        }
        Material material = state.getMaterial();
        if (material.blocksMovement() || material == Material.PORTAL || material == Material.STRUCTURE_VOID) return false;
        int otherDensity = getDensity(world, pos);
        if (otherDensity == Integer.MAX_VALUE) {
            block.dropBlockAsItemWithChance(world, pos, state, 1.0F, 0);
            return true;
        }
        return this.density > otherDensity;
    }

    public abstract int getQuantaValue(IBlockAccess world, BlockPos pos);

    @Override
    public abstract boolean canCollideCheck(IBlockState state, boolean fullHit);

    public abstract int getMaxRenderHeightMeta();

    // ------------------------------------------------------------------ ticking

    @Override
    public void onBlockAdded(World world, BlockPos pos, IBlockState state) {
        world.scheduleUpdate(pos, this, tickRate);
    }

    @Override
    public void neighborChanged(IBlockState state, World world, BlockPos pos, Block neighborBlock, BlockPos neighbourPos) {
        world.scheduleUpdate(pos, this, tickRate);
    }

    @Override
    public int tickRate(World world) {
        return tickRate;
    }

    // ------------------------------------------------------------------ fluid queries

    /** The density of the fluid block at a position (MAX_VALUE for anything that is not one). */
    public static int getDensity(IBlockAccess world, BlockPos pos) {
        IBlockState state = world.getBlockState(pos);
        if (!(state.getBlock() instanceof BlockFluidBase f)) return Integer.MAX_VALUE;
        return f.density;
    }

    public static int getTemperature(IBlockAccess world, BlockPos pos) {
        IBlockState state = world.getBlockState(pos);
        if (!(state.getBlock() instanceof BlockFluidBase f)) return Integer.MAX_VALUE;
        return f.temperature;
    }

    /** The angle the fluid flows at a position (-1000 when still), for the flowing texture. */
    public static double getFlowDirection(IBlockAccess world, BlockPos pos) {
        IBlockState state = world.getBlockState(pos);
        if (!(state.getBlock() instanceof BlockFluidBase fluid)) return -1000.0;
        Vec3d vec = fluid.getFlowVector(world, pos);
        return vec.x == 0.0D && vec.z == 0.0D ? -1000.0D : Math.atan2(vec.z, vec.x) - Math.PI / 2D;
    }

    public final int getDensity() {
        return density;
    }

    /** The surface height of the fluid at a position for rendering (1 under more fluid). */
    public float getFluidHeightForRender(IBlockAccess world, BlockPos pos, IBlockState up) {
        IBlockState here = world.getBlockState(pos);
        IBlockState above = world.getBlockState(pos.down(densityDir));
        if (above.getBlock() == this) return 1.0F;
        if (here.getBlock() == this) {
            int level = here.getValue(LEVEL);
            if (level == getMaxRenderHeightMeta()) return quantaFraction;
            return (quantaPerBlock - level) / quantaPerBlockFloat * quantaFraction;
        }
        return !here.getMaterial().isSolid() && above.getBlock() == this ? 1.0F : getQuantaPercentage(world, pos) * quantaFraction;
    }

    public float getQuantaPercentage(IBlockAccess world, BlockPos pos) {
        int quantaRemaining = getQuantaValue(world, pos);
        return quantaRemaining / quantaPerBlockFloat;
    }

    /** The direction the fluid flows at a position, from the levels around it. */
    public Vec3d getFlowVector(IBlockAccess world, BlockPos pos) {
        double x = 0.0D;
        double z = 0.0D;
        int decay = getFlowDecay(world, pos);
        for (EnumFacing side : new EnumFacing[] {EnumFacing.NORTH, EnumFacing.SOUTH, EnumFacing.WEST, EnumFacing.EAST}) {
            BlockPos other = pos.offset(side);
            int otherDecay = getFlowDecay(world, other);
            if (otherDecay >= quantaPerBlock) {
                if (!world.getBlockState(other).getMaterial().blocksMovement()) {
                    otherDecay = getFlowDecay(world, other.up(densityDir));
                    if (otherDecay < quantaPerBlock) {
                        int power = otherDecay - (decay - quantaPerBlock);
                        x += side.getXOffset() * power;
                        z += side.getZOffset() * power;
                    }
                }
            } else {
                int power = otherDecay - decay;
                x += side.getXOffset() * power;
                z += side.getZOffset() * power;
            }
        }
        Vec3d vec = new Vec3d(x, 0.0D, z);
        return vec.x == 0.0D && vec.z == 0.0D ? vec : vec.normalize();
    }

    private int getFlowDecay(IBlockAccess world, BlockPos pos) {
        int quantaValue = getQuantaValue(world, pos);
        return quantaValue > 0 && world.getBlockState(pos.up(-densityDir)).getBlock() == this ? 0 : quantaPerBlock - quantaValue;
    }

    /** Pushes entities in the fluid along its flow (fluids lighter than air do not push). */
    public Vec3d modifyAcceleration(World world, BlockPos pos, Entity entity, Vec3d vec) {
        return densityDir > 0 ? vec : vec.add(getFlowVector(world, pos));
    }

    /** How full the block is (-1..1, negative for gases). */
    @Override
    public float getFilledPercentage(World world, BlockPos pos) {
        int quantaRemaining = getQuantaValue(world, pos);
        float remaining = (quantaRemaining + 1.0F) / (quantaPerBlockFloat + 1.0F);
        return remaining * (density > 0 ? 1 : -1);
    }

    public int getQuantaPerBlock() {
        return quantaPerBlock;
    }

    public int getDensityDir() {
        return densityDir;
    }

    /** Fluids are drawn by the fluid renderer of the layer, not as block models. */
    @Override
    public legacyconnector.v1_12_2.net.minecraft.util.EnumBlockRenderType getRenderType(IBlockState state) {
        return legacyconnector.v1_12_2.net.minecraft.util.EnumBlockRenderType.MODEL;
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return false;
    }

    @Override
    public legacyconnector.v1_12_2.net.minecraft.util.math.AxisAlignedBB getCollisionBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
        return NULL_AABB;
    }

    @Override
    public boolean isReplaceable(IBlockAccess world, BlockPos pos) {
        return true;
    }

    static {
        for (String name : new String[] {"wooden_door", "spruce_door", "birch_door", "jungle_door", "acacia_door", "dark_oak_door", "iron_door", "standing_sign",
                "wall_sign", "reeds"}) {
            try {
                Block b = Block.getBlockFromName(name);
                if (b != null) defaultDisplacements.put(b, false);
            } catch (RuntimeException | LinkageError e) {
                // left to its material
            }
        }
    }

    @Override
    public BlockRenderLayer getRenderLayer() {
        return renderLayer;
    }

    /** A face is drawn unless the neighbour there is the same fluid (or a solid block, except on top). */
    @Override
    public boolean shouldSideBeRendered(IBlockState state, IBlockAccess world, BlockPos pos, EnumFacing side) {
        IBlockState neighbor = world.getBlockState(pos.offset(side));
        if (neighbor.getMaterial() == state.getMaterial()) return false;
        if (side == (densityDir < 0 ? EnumFacing.UP : EnumFacing.DOWN)) return true;
        return super.shouldSideBeRendered(state, world, pos, side);
    }

    /** The average of corner heights that are not air (-1 marks air), for the rendered surface. */
    protected float getFluidHeightAverage(float... flow) {
        float total = 0;
        int count = 0;
        for (float f : flow) {
            if (f >= quantaFraction) {
                total += f * 10;
                count += 10;
            }
            if (f >= 0) {
                total += f;
                count++;
            }
        }
        return count == 0 ? 0 : total / count;
    }

    /** Forge: whether an entity's eyes (or body) are inside this fluid at the height it has here. */
    public Boolean isEntityInsideMaterial(IBlockAccess world, BlockPos blockpos, IBlockState iblockstate, Entity entity, double yToTest,
            Material materialIn, boolean testingHead) {
        if (materialIn == null || materialIn != getDefaultState().getMaterial()) return null;
        if (!testingHead) return true;
        float height = getFluidHeightForRender(world, blockpos, iblockstate);
        double top = blockpos.getY() + (densityDir < 0 ? height : 1.0D - height);
        return densityDir < 0 ? yToTest < top : yToTest > top;
    }
}
