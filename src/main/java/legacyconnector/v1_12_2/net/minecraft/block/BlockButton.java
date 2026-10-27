package legacyconnector.v1_12_2.net.minecraft.block;

import java.util.Random;
import legacyconnector.v1_12_2.net.minecraft.block.material.Material;
import legacyconnector.v1_12_2.net.minecraft.block.properties.PropertyBool;
import legacyconnector.v1_12_2.net.minecraft.block.state.BlockFaceShape;
import legacyconnector.v1_12_2.net.minecraft.block.state.BlockStateContainer;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import legacyconnector.v1_12_2.net.minecraft.util.EnumHand;
import legacyconnector.v1_12_2.net.minecraft.util.Mirror;
import legacyconnector.v1_12_2.net.minecraft.util.Rotation;
import legacyconnector.v1_12_2.net.minecraft.util.math.AxisAlignedBB;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.IBlockAccess;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/** 1.12.2 buttons: pressed by hand, they power the block they sit on for 1.5 s (wood) or 1 s (stone), then pop out. */
public abstract class BlockButton extends BlockDirectional {
    public static final PropertyBool POWERED = PropertyBool.create("powered");
    protected static final AxisAlignedBB AABB_DOWN_OFF = new AxisAlignedBB(0.3125D, 0.875D, 0.375D, 0.6875D, 1.0D, 0.625D);
    protected static final AxisAlignedBB AABB_UP_OFF = new AxisAlignedBB(0.3125D, 0.0D, 0.375D, 0.6875D, 0.125D, 0.625D);
    protected static final AxisAlignedBB AABB_NORTH_OFF = new AxisAlignedBB(0.3125D, 0.375D, 0.875D, 0.6875D, 0.625D, 1.0D);
    protected static final AxisAlignedBB AABB_SOUTH_OFF = new AxisAlignedBB(0.3125D, 0.375D, 0.0D, 0.6875D, 0.625D, 0.125D);
    protected static final AxisAlignedBB AABB_WEST_OFF = new AxisAlignedBB(0.875D, 0.375D, 0.3125D, 1.0D, 0.625D, 0.6875D);
    protected static final AxisAlignedBB AABB_EAST_OFF = new AxisAlignedBB(0.0D, 0.375D, 0.3125D, 0.125D, 0.625D, 0.6875D);
    protected static final AxisAlignedBB AABB_DOWN_ON = new AxisAlignedBB(0.3125D, 0.9375D, 0.375D, 0.6875D, 1.0D, 0.625D);
    protected static final AxisAlignedBB AABB_UP_ON = new AxisAlignedBB(0.3125D, 0.0D, 0.375D, 0.6875D, 0.0625D, 0.625D);
    protected static final AxisAlignedBB AABB_NORTH_ON = new AxisAlignedBB(0.3125D, 0.375D, 0.9375D, 0.6875D, 0.625D, 1.0D);
    protected static final AxisAlignedBB AABB_SOUTH_ON = new AxisAlignedBB(0.3125D, 0.375D, 0.0D, 0.6875D, 0.625D, 0.0625D);
    protected static final AxisAlignedBB AABB_WEST_ON = new AxisAlignedBB(0.9375D, 0.375D, 0.3125D, 1.0D, 0.625D, 0.6875D);
    protected static final AxisAlignedBB AABB_EAST_ON = new AxisAlignedBB(0.0D, 0.375D, 0.3125D, 0.0625D, 0.625D, 0.6875D);
    private final boolean wooden;

    protected BlockButton(boolean wooden) {
        super(Material.CIRCUITS);
        setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH).withProperty(POWERED, false));
        setTickRandomly(true);
        setCreativeTab(legacyconnector.v1_12_2.net.minecraft.creativetab.CreativeTabs.REDSTONE);
        this.wooden = wooden;
    }

    @Override
    public AxisAlignedBB getCollisionBoundingBox(IBlockState blockState, IBlockAccess worldIn, BlockPos pos) {
        return NULL_AABB;
    }

    @Override
    public int tickRate(World worldIn) {
        return wooden ? 30 : 20;
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
    public boolean canPlaceBlockOnSide(World worldIn, BlockPos pos, EnumFacing side) {
        return canPlaceBlock(worldIn, pos, side);
    }

    @Override
    public boolean canPlaceBlockAt(World worldIn, BlockPos pos) {
        for (EnumFacing enumfacing : EnumFacing.values()) if (canPlaceBlock(worldIn, pos, enumfacing)) return true;
        return false;
    }

    /** Whether the block behind (opposite the facing) holds the button: a solid face. */
    protected static boolean canPlaceBlock(World worldIn, BlockPos pos, EnumFacing direction) {
        BlockPos blockpos = pos.offset(direction.getOpposite());
        IBlockState iblockstate = worldIn.getBlockState(blockpos);
        boolean solid = iblockstate.getBlockFaceShape(worldIn, blockpos, direction) == BlockFaceShape.SOLID;
        Block block = iblockstate.getBlock();
        if (direction == EnumFacing.UP) return iblockstate.isTopSolid() || !isExceptionBlockForAttaching(block) && solid;
        return !isExceptBlockForAttachWithPiston(block) && solid;
    }

    protected static boolean isExceptionBlockForAttaching(Block attachBlock) {
        var name = attachBlock == null ? null : attachBlock.getRegistryName();
        return name != null && "minecraft".equals(name.getNamespace()) && name.getPath().endsWith("shulker_box")
                || Block.mcc$named(attachBlock, "beacon", "cauldron", "glass", "stained_glass", "piston", "sticky_piston", "piston_head");
    }

    @Override
    public IBlockState getStateForPlacement(World worldIn, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta,
            EntityLivingBase placer) {
        return canPlaceBlock(worldIn, pos, facing) ? getDefaultState().withProperty(FACING, facing).withProperty(POWERED, false)
                : getDefaultState().withProperty(FACING, EnumFacing.DOWN).withProperty(POWERED, false);
    }

    @Override
    public void neighborChanged(IBlockState state, World worldIn, BlockPos pos, Block blockIn, BlockPos fromPos) {
        if (checkForDrop(worldIn, pos, state) && !canPlaceBlock(worldIn, pos, state.getValue(FACING))) {
            dropBlockAsItem(worldIn, pos, state, 0);
            worldIn.setBlockToAir(pos);
        }
    }

    private boolean checkForDrop(World worldIn, BlockPos pos, IBlockState state) {
        if (canPlaceBlockAt(worldIn, pos)) return true;
        dropBlockAsItem(worldIn, pos, state, 0);
        worldIn.setBlockToAir(pos);
        return false;
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        return mcc$staticBounds(state);
    }

    @Override
    public AxisAlignedBB mcc$staticBounds(IBlockState state) {
        boolean on = state.getValue(POWERED);
        return switch (state.getValue(FACING)) {
            case EAST -> on ? AABB_EAST_ON : AABB_EAST_OFF;
            case WEST -> on ? AABB_WEST_ON : AABB_WEST_OFF;
            case SOUTH -> on ? AABB_SOUTH_ON : AABB_SOUTH_OFF;
            case UP -> on ? AABB_UP_ON : AABB_UP_OFF;
            case DOWN -> on ? AABB_DOWN_ON : AABB_DOWN_OFF;
            default -> on ? AABB_NORTH_ON : AABB_NORTH_OFF;
        };
    }

    @Override
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn, EnumHand hand, EnumFacing facing, float hitX,
            float hitY, float hitZ) {
        if (state.getValue(POWERED)) return true;
        worldIn.setBlockState(pos, state.withProperty(POWERED, true), 3);
        worldIn.markBlockRangeForRenderUpdate(pos, pos);
        playClickSound(playerIn, worldIn, pos);
        notifyNeighbors(worldIn, pos, state.getValue(FACING));
        worldIn.scheduleUpdate(pos, this, tickRate(worldIn));
        return true;
    }

    protected abstract void playClickSound(EntityPlayer player, World worldIn, BlockPos pos);

    protected abstract void playReleaseSound(World worldIn, BlockPos pos);

    @Override
    public void breakBlock(World worldIn, BlockPos pos, IBlockState state) {
        if (state.getValue(POWERED)) notifyNeighbors(worldIn, pos, state.getValue(FACING));
        super.breakBlock(worldIn, pos, state);
    }

    @Override
    public int getWeakPower(IBlockState blockState, IBlockAccess blockAccess, BlockPos pos, EnumFacing side) {
        return blockState.getValue(POWERED) ? 15 : 0;
    }

    @Override
    public int getStrongPower(IBlockState blockState, IBlockAccess blockAccess, BlockPos pos, EnumFacing side) {
        if (!blockState.getValue(POWERED)) return 0;
        return blockState.getValue(FACING) == side ? 15 : 0;
    }

    @Override
    public boolean canProvidePower(IBlockState state) {
        return true;
    }

    @Override
    public void randomTick(World worldIn, BlockPos pos, IBlockState state, Random random) {
    }

    /** Pops the button back out when its time is up. */
    @Override
    public void updateTick(World worldIn, BlockPos pos, IBlockState state, Random rand) {
        if (worldIn.isRemote || !state.getValue(POWERED)) return;
        worldIn.setBlockState(pos, state.withProperty(POWERED, false));
        notifyNeighbors(worldIn, pos, state.getValue(FACING));
        playReleaseSound(worldIn, pos);
        worldIn.markBlockRangeForRenderUpdate(pos, pos);
    }

    private void notifyNeighbors(World worldIn, BlockPos pos, EnumFacing facing) {
        worldIn.notifyNeighborsOfStateChange(pos, this, false);
        worldIn.notifyNeighborsOfStateChange(pos.offset(facing.getOpposite()), this, false);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        EnumFacing enumfacing = switch (meta & 7) {
            case 0 -> EnumFacing.DOWN;
            case 1 -> EnumFacing.EAST;
            case 2 -> EnumFacing.WEST;
            case 3 -> EnumFacing.SOUTH;
            case 4 -> EnumFacing.NORTH;
            default -> EnumFacing.UP;
        };
        return getDefaultState().withProperty(FACING, enumfacing).withProperty(POWERED, (meta & 8) > 0);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        int i = switch (state.getValue(FACING)) {
            case EAST -> 1;
            case WEST -> 2;
            case SOUTH -> 3;
            case NORTH -> 4;
            case DOWN -> 0;
            default -> 5;
        };
        if (state.getValue(POWERED)) i |= 8;
        return i;
    }

    @Override
    public IBlockState withRotation(IBlockState state, Rotation rot) {
        return state.withProperty(FACING, rot.rotate(state.getValue(FACING)));
    }

    @Override
    public IBlockState withMirror(IBlockState state, Mirror mirrorIn) {
        return state.withRotation(mirrorIn.toRotation(state.getValue(FACING)));
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, FACING, POWERED);
    }

    @Override
    public BlockFaceShape getBlockFaceShape(IBlockAccess worldIn, IBlockState state, BlockPos pos, EnumFacing face) {
        return BlockFaceShape.UNDEFINED;
    }
}
