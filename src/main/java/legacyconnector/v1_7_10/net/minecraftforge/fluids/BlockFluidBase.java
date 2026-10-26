package legacyconnector.v1_7_10.net.minecraftforge.fluids;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import legacyconnector.v1_7_10.net.minecraft.block.Block;
import legacyconnector.v1_7_10.net.minecraft.block.material.Material;
import legacyconnector.v1_7_10.net.minecraft.entity.Entity;
import legacyconnector.v1_7_10.net.minecraft.init.Blocks;
import legacyconnector.v1_7_10.net.minecraft.util.AxisAlignedBB;
import legacyconnector.v1_7_10.net.minecraft.util.IIcon;
import legacyconnector.v1_7_10.net.minecraft.util.Vec3;
import legacyconnector.v1_7_10.net.minecraft.world.IBlockAccess;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/**
 * Forge 1.7.10 base of fluid blocks: a block whose metadata is the fluid's level (0 = a full source block), flowing
 * by scheduled updates ({@link BlockFluidClassic}, {@link BlockFluidFinite}).
 */
public abstract class BlockFluidBase extends Block implements IFluidBlock {
    protected static final Map<Block, Boolean> defaultDisplacements = new HashMap<>();

    static {
        defaultDisplacements.put(Blocks.wooden_door, false);
        defaultDisplacements.put(Blocks.iron_door, false);
        defaultDisplacements.put(Blocks.standing_sign, false);
        defaultDisplacements.put(Blocks.wall_sign, false);
        defaultDisplacements.put(Blocks.reeds, false);
    }

    protected Map<Block, Boolean> displacements = new HashMap<>(defaultDisplacements);

    protected int quantaPerBlock = 8;
    protected float quantaPerBlockFloat = 8F;
    protected int density = 1;
    protected int densityDir = -1;
    protected int temperature = 295;
    protected int tickRate = 20;
    protected int renderPass = 0;
    protected int maxScaledLight = 0;
    protected final String fluidName;

    public BlockFluidBase(Fluid fluid, Material material) {
        super(material);
        this.setTickRandomly(false);
        this.disableStats();
        this.fluidName = fluid.getName();
        this.density = fluid.density;
        this.temperature = fluid.temperature;
        this.maxScaledLight = fluid.luminosity;
        this.tickRate = Math.max(1, fluid.viscosity / 200);
        this.densityDir = fluid.density > 0 ? -1 : 1;
        this.renderPass = material == Material.water || fluid.getName().equals("water") ? 1 : 0;
        displacements.put(this, false);
    }

    public BlockFluidBase setQuantaPerBlock(int quantaPerBlock) {
        if (quantaPerBlock > 16 || quantaPerBlock < 1) quantaPerBlock = 8;
        this.quantaPerBlock = quantaPerBlock;
        this.quantaPerBlockFloat = quantaPerBlock;
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

    public BlockFluidBase setTickRate(int tickRate) {
        if (tickRate <= 0) tickRate = 20;
        this.tickRate = tickRate;
        return this;
    }

    public BlockFluidBase setRenderPass(int renderPass) {
        this.renderPass = renderPass;
        return this;
    }

    public BlockFluidBase setMaxScaledLight(int maxScaledLight) {
        this.maxScaledLight = maxScaledLight;
        return this;
    }

    public final int getDensity() {
        return density;
    }

    public final int getTemperature() {
        return temperature;
    }

    public final int getMaxRenderHeightMeta() {
        return 0;
    }

    public final int getQuantaPerBlock() {
        return quantaPerBlock;
    }

    public int getDensity(IBlockAccess world, int x, int y, int z) {
        Block block = world.getBlock(x, y, z);
        if (block == null || block == Blocks.air) return Integer.MAX_VALUE;
        if (block instanceof BlockFluidBase f) return f.density;
        return Integer.MAX_VALUE;
    }

    public BlockFluidBase setFluidStack(FluidStack stack) {
        return this;
    }

    public static boolean isFluidBlock(Block block) {
        return block instanceof BlockFluidBase || block instanceof IFluidBlock;
    }

    public abstract int getQuantaValue(IBlockAccess world, int x, int y, int z);

    public abstract boolean canCollideCheck(int meta, boolean fullHit);

    public abstract int getMaxRenderHeightMeta(IBlockAccess world, int x, int y, int z);

    public float getQuantaPercentage(IBlockAccess world, int x, int y, int z) {
        int quantaRemaining = getQuantaValue(world, x, y, z);
        return quantaRemaining / quantaPerBlockFloat;
    }

    // ------------------------------------------------------------------ the block

    @Override
    public boolean isOpaqueCube() {
        return false;
    }

    @Override
    public boolean renderAsNormalBlock() {
        return false;
    }

    @Override
    public boolean isCollidable() {
        return true;
    }

    @Override
    public boolean isReplaceable(IBlockAccess world, int x, int y, int z) {
        return true;
    }

    @Override
    public AxisAlignedBB getCollisionBoundingBoxFromPool(World world, int x, int y, int z) {
        return null;
    }

    @Override
    public int getRenderType() {
        return FluidRegistry.renderIdFluid;
    }

    @Override
    public int getRenderBlockPass() {
        return renderPass;
    }

    @Override
    public int quantityDropped(Random random) {
        return 0;
    }

    @Override
    public legacyconnector.v1_7_10.net.minecraft.item.Item getItemDropped(int meta, Random random, int fortune) {
        return null;
    }

    @Override
    public int tickRate(World world) {
        return tickRate;
    }

    @Override
    public IIcon getIcon(int side, int meta) {
        Fluid fluid = FluidRegistry.getFluid(fluidName);
        if (fluid == null) return null;
        return side != 0 && side != 1 ? fluid.getFlowingIcon() : fluid.getStillIcon();
    }

    @Override
    public void onBlockAdded(World world, int x, int y, int z) {
        world.scheduleBlockUpdate(x, y, z, this, tickRate(world));
    }

    @Override
    public void onNeighborBlockChange(World world, int x, int y, int z, Block block) {
        world.scheduleBlockUpdate(x, y, z, this, tickRate(world));
    }

    @Override
    public int getLightValue(IBlockAccess world, int x, int y, int z) {
        return maxScaledLight;
    }

    @Override
    public void onEntityCollidedWithBlock(World world, int x, int y, int z, Entity entity) {
        if (entity == null) return;
        // Fluids slow what is in them (1.7.10 did this through the player's "in water" handling for its own blocks).
        double drag = Math.min(0.95D, 0.8D + (1000 - Math.min(1000, Math.max(0, density))) / 10000.0D);
        Entity.mcc$set$motionX(entity, Entity.mcc$get$motionX(entity) * drag);
        Entity.mcc$set$motionZ(entity, Entity.mcc$get$motionZ(entity) * drag);
        double my = Entity.mcc$get$motionY(entity);
        if (my < 0) Entity.mcc$set$motionY(entity, my * drag);
        if (temperature >= 1000) entity.setFire(5);
    }

    // ------------------------------------------------------------------ flow direction (renderers and entity pushing)

    /**
     * The angle of the flow at a position (what fluid renderers rotate the flowing texture by), or -1000 where it is still or
     * not a liquid. Vanilla liquids answer through {@link legacyconnector.v1_7_10.net.minecraft.block.BlockLiquid}.
     */
    public static double getFlowDirection(IBlockAccess world, int x, int y, int z) {
        Block block = world.getBlock(x, y, z);
        if (block == null || !block.getMaterial().isLiquid()) return -1000.0D;
        if (!(block instanceof BlockFluidBase fluid)) {
            return legacyconnector.v1_7_10.net.minecraft.block.BlockLiquid.getFlowDirection(world, x, y, z, block.getMaterial());
        }
        Vec3 vec = fluid.getFlowVector(world, x, y, z);
        return vec.xCoord == 0.0D && vec.zCoord == 0.0D ? -1000.0D : Math.atan2(vec.zCoord, vec.xCoord) - Math.PI / 2.0D;
    }

    /** The normalised direction the fluid flows in at a position: towards lower quanta, and down along walls it falls past. */
    public Vec3 getFlowVector(IBlockAccess world, int x, int y, int z) {
        Vec3 vec = Vec3.createVectorHelper(0.0D, 0.0D, 0.0D);
        int decay = quantaPerBlock - getQuantaValue(world, x, y, z);
        for (int side = 0; side < 4; ++side) {
            int x2 = x;
            int z2 = z;
            switch (side) {
                case 0 -> --x2;
                case 1 -> --z2;
                case 2 -> ++x2;
                default -> ++z2;
            }
            int otherDecay = quantaPerBlock - getQuantaValue(world, x2, y, z2);
            if (otherDecay >= quantaPerBlock) {
                if (!world.getBlock(x2, y, z2).getMaterial().blocksMovement()) {
                    otherDecay = quantaPerBlock - getQuantaValue(world, x2, y - 1, z2);
                    if (otherDecay >= 0) {
                        int power = otherDecay - (decay - quantaPerBlock);
                        vec = vec.addVector((x2 - x) * power, 0.0D, (z2 - z) * power);
                    }
                }
            } else if (otherDecay >= 0) {
                int power = otherDecay - decay;
                vec = vec.addVector((x2 - x) * power, 0.0D, (z2 - z) * power);
            }
        }
        if (world.getBlock(x, y + 1, z) == this) {
            boolean wall = isBlockSolid(world, x, y, z - 1, 2) || isBlockSolid(world, x, y, z + 1, 3) || isBlockSolid(world, x - 1, y, z, 4)
                    || isBlockSolid(world, x + 1, y, z, 5) || isBlockSolid(world, x, y + 1, z - 1, 2) || isBlockSolid(world, x, y + 1, z + 1, 3)
                    || isBlockSolid(world, x - 1, y + 1, z, 4) || isBlockSolid(world, x + 1, y + 1, z, 5);
            if (wall) vec = vec.normalize().addVector(0.0D, -6.0D, 0.0D);
        }
        return vec.normalize();
    }

    /** Entities in a falling (dense) fluid are pushed along its flow. */
    @Override
    public void velocityToAddToEntity(World world, int x, int y, int z, Entity entity, Vec3 vec) {
        if (densityDir > 0 || vec == null) return;
        Vec3 flow = getFlowVector(world, x, y, z);
        vec.xCoord += flow.xCoord * (quantaPerBlock * 4);
        vec.yCoord += flow.yCoord * (quantaPerBlock * 4);
        vec.zCoord += flow.zCoord * (quantaPerBlock * 4);
    }

    // ------------------------------------------------------------------ helpers shared by the flows

    protected boolean canDisplace(IBlockAccess world, int x, int y, int z) {
        Block block = world.getBlock(x, y, z);
        if (block == null || block == Blocks.air || block.isAir(world, x, y, z)) return true;
        if (block == this) return false;
        Boolean explicit = displacements.get(block);
        if (explicit != null) return explicit;
        Material material = block.getMaterial();
        if (material.blocksMovement() || material == Material.portal) return false;
        int otherDensity = getDensity(world, x, y, z);
        if (otherDensity == Integer.MAX_VALUE) return true;
        return this.density > otherDensity;
    }

    protected boolean displaceIfPossible(World world, int x, int y, int z) {
        Block block = world.getBlock(x, y, z);
        if (block == null || block == Blocks.air || block.isAir(world, x, y, z)) return true;
        if (block == this) return false;
        if (!canDisplace(world, x, y, z)) return false;
        block.dropBlockAsItem(world, x, y, z, world.getBlockMetadata(x, y, z), 0);
        return true;
    }

    public FluidStack drain(World world, int x, int y, int z, boolean doDrain) {
        return null;
    }

    public boolean canDrain(World world, int x, int y, int z) {
        return false;
    }

    public float getFilledPercentage(World world, int x, int y, int z) {
        return 1;
    }

    @Override
    public Fluid getFluid() {
        return FluidRegistry.getFluid(fluidName);
    }
}
