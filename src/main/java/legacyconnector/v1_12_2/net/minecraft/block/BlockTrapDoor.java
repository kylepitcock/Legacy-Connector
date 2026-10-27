package legacyconnector.v1_12_2.net.minecraft.block;

import legacyconnector.v1_12_2.net.minecraft.block.material.Material;
import legacyconnector.v1_12_2.net.minecraft.block.properties.PropertyBool;
import legacyconnector.v1_12_2.net.minecraft.block.properties.PropertyDirection;
import legacyconnector.v1_12_2.net.minecraft.block.properties.PropertyEnum;
import legacyconnector.v1_12_2.net.minecraft.block.state.BlockFaceShape;
import legacyconnector.v1_12_2.net.minecraft.block.state.BlockStateContainer;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.util.BlockRenderLayer;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import legacyconnector.v1_12_2.net.minecraft.util.EnumHand;
import legacyconnector.v1_12_2.net.minecraft.util.IStringSerializable;
import legacyconnector.v1_12_2.net.minecraft.util.Mirror;
import legacyconnector.v1_12_2.net.minecraft.util.Rotation;
import legacyconnector.v1_12_2.net.minecraft.util.math.AxisAlignedBB;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.IBlockAccess;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/** 1.12.2 trapdoors: a slab-thin hatch on the top or bottom half that swings up against its facing side; by hand (not iron) or redstone. */
public class BlockTrapDoor extends Block {
    public static final PropertyDirection FACING = BlockHorizontal.FACING;
    public static final PropertyBool OPEN = PropertyBool.create("open");
    public static final PropertyEnum<DoorHalf> HALF = PropertyEnum.create("half", DoorHalf.class);
    protected static final AxisAlignedBB EAST_OPEN_AABB = new AxisAlignedBB(0.0D, 0.0D, 0.0D, 0.1875D, 1.0D, 1.0D);
    protected static final AxisAlignedBB WEST_OPEN_AABB = new AxisAlignedBB(0.8125D, 0.0D, 0.0D, 1.0D, 1.0D, 1.0D);
    protected static final AxisAlignedBB SOUTH_OPEN_AABB = new AxisAlignedBB(0.0D, 0.0D, 0.0D, 1.0D, 1.0D, 0.1875D);
    protected static final AxisAlignedBB NORTH_OPEN_AABB = new AxisAlignedBB(0.0D, 0.0D, 0.8125D, 1.0D, 1.0D, 1.0D);
    protected static final AxisAlignedBB BOTTOM_AABB = new AxisAlignedBB(0.0D, 0.0D, 0.0D, 1.0D, 0.1875D, 1.0D);
    protected static final AxisAlignedBB TOP_AABB = new AxisAlignedBB(0.0D, 0.8125D, 0.0D, 1.0D, 1.0D, 1.0D);

    protected BlockTrapDoor(Material materialIn) {
        super(materialIn);
        setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH).withProperty(OPEN, false).withProperty(HALF, DoorHalf.BOTTOM));
        setCreativeTab(legacyconnector.v1_12_2.net.minecraft.creativetab.CreativeTabs.REDSTONE);
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        return mcc$staticBounds(state);
    }

    @Override
    public AxisAlignedBB mcc$staticBounds(IBlockState state) {
        if (state.getValue(OPEN)) {
            return switch (state.getValue(FACING)) {
                case SOUTH -> SOUTH_OPEN_AABB;
                case WEST -> WEST_OPEN_AABB;
                case EAST -> EAST_OPEN_AABB;
                default -> NORTH_OPEN_AABB;
            };
        }
        return state.getValue(HALF) == DoorHalf.TOP ? TOP_AABB : BOTTOM_AABB;
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
    public boolean isPassable(IBlockAccess worldIn, BlockPos pos) {
        return !worldIn.getBlockState(pos).getValue(OPEN);
    }

    @Override
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn, EnumHand hand, EnumFacing facing, float hitX,
            float hitY, float hitZ) {
        if (getMaterialLegacy() == Material.IRON) return false;
        state = state.cycleProperty(OPEN);
        worldIn.setBlockState(pos, state, 2);
        playSound(playerIn, worldIn, pos, state.getValue(OPEN));
        return true;
    }

    protected void playSound(EntityPlayer player, World worldIn, BlockPos pos, boolean open) {
        int event = open ? (getMaterialLegacy() == Material.IRON ? 1037 : 1007) : (getMaterialLegacy() == Material.IRON ? 1036 : 1013);
        worldIn.playEvent(player, event, pos, 0);
    }

    @Override
    public void neighborChanged(IBlockState state, World worldIn, BlockPos pos, Block blockIn, BlockPos fromPos) {
        if (worldIn.isRemote) return;
        boolean powered = worldIn.isBlockPowered(pos);
        if (powered || blockIn.getDefaultState().canProvidePower()) {
            boolean open = state.getValue(OPEN);
            if (open != powered) {
                worldIn.setBlockState(pos, state.withProperty(OPEN, powered), 2);
                playSound(null, worldIn, pos, powered);
            }
        }
    }

    @Override
    public IBlockState getStateForPlacement(World worldIn, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta,
            EntityLivingBase placer) {
        IBlockState iblockstate = getDefaultState();
        if (facing.getAxis().isHorizontal()) {
            iblockstate = iblockstate.withProperty(FACING, facing).withProperty(OPEN, false).withProperty(HALF, hitY > 0.5F ? DoorHalf.TOP : DoorHalf.BOTTOM);
        } else {
            iblockstate = iblockstate.withProperty(FACING, placer.getHorizontalFacing().getOpposite()).withProperty(OPEN, false)
                    .withProperty(HALF, facing == EnumFacing.UP ? DoorHalf.BOTTOM : DoorHalf.TOP);
        }
        if (worldIn.isBlockPowered(pos)) iblockstate = iblockstate.withProperty(OPEN, true);
        return iblockstate;
    }

    @Override
    public boolean canPlaceBlockOnSide(World worldIn, BlockPos pos, EnumFacing side) {
        return true;
    }

    @Override
    public BlockRenderLayer getRenderLayer() {
        return BlockRenderLayer.CUTOUT;
    }

    protected static EnumFacing getFacing(int meta) {
        return switch (meta & 3) {
            case 0 -> EnumFacing.NORTH;
            case 1 -> EnumFacing.SOUTH;
            case 2 -> EnumFacing.WEST;
            default -> EnumFacing.EAST;
        };
    }

    protected static int getMetaForFacing(EnumFacing facing) {
        return switch (facing) {
            case SOUTH -> 1;
            case WEST -> 2;
            case EAST -> 3;
            default -> 0;
        };
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState().withProperty(FACING, getFacing(meta)).withProperty(OPEN, (meta & 4) != 0)
                .withProperty(HALF, (meta & 8) == 0 ? DoorHalf.BOTTOM : DoorHalf.TOP);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        int i = getMetaForFacing(state.getValue(FACING));
        if (state.getValue(OPEN)) i |= 4;
        if (state.getValue(HALF) == DoorHalf.TOP) i |= 8;
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
        return new BlockStateContainer(this, FACING, OPEN, HALF);
    }

    @Override
    public BlockFaceShape getBlockFaceShape(IBlockAccess worldIn, IBlockState state, BlockPos pos, EnumFacing face) {
        return (face == EnumFacing.UP && state.getValue(HALF) == DoorHalf.TOP || face == EnumFacing.DOWN && state.getValue(HALF) == DoorHalf.BOTTOM)
                && !state.getValue(OPEN) ? BlockFaceShape.SOLID : BlockFaceShape.UNDEFINED;
    }

    /** Forge: an open trapdoor above a ladder is climbable. */
    @Override
    public boolean isLadder(IBlockState state, IBlockAccess world, BlockPos pos, legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase entity) {
        if (state.getValue(OPEN)) {
            IBlockState down = world.getBlockState(pos.down());
            if (down.getBlock() instanceof BlockLadder) return down.getValue(BlockLadder.FACING) == state.getValue(FACING);
            return Block.mcc$named(down.getBlock(), "ladder");
        }
        return false;
    }

    public enum DoorHalf implements IStringSerializable {
        TOP("top"),
        BOTTOM("bottom");

        private final String name;

        DoorHalf(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }

        @Override
        public String getName() {
            return name;
        }
    }
}
