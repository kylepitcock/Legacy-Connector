package legacyconnector.v1_12_2.net.minecraft.block;

import legacyconnector.v1_12_2.net.minecraft.block.properties.PropertyDirection;
import legacyconnector.v1_12_2.net.minecraft.block.properties.PropertyEnum;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import java.util.ArrayList;
import java.util.List;
import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import legacyconnector.v1_12_2.net.minecraft.util.IStringSerializable;
import legacyconnector.v1_12_2.net.minecraft.util.math.AxisAlignedBB;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.util.math.RayTraceResult;
import legacyconnector.v1_12_2.net.minecraft.util.math.Vec3d;
import legacyconnector.v1_12_2.net.minecraft.world.IBlockAccess;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/** The stairs base class; mods extend it for their own stairs (and test vanilla blocks against it). */
public class BlockStairs extends Block {
    public static final PropertyDirection FACING = PropertyDirection.create("facing", java.util.List.of(
            legacyconnector.v1_12_2.net.minecraft.util.EnumFacing.NORTH, legacyconnector.v1_12_2.net.minecraft.util.EnumFacing.SOUTH,
            legacyconnector.v1_12_2.net.minecraft.util.EnumFacing.WEST, legacyconnector.v1_12_2.net.minecraft.util.EnumFacing.EAST));
    public static final PropertyEnum<EnumHalf> HALF = PropertyEnum.create("half", EnumHalf.class);
    public static final PropertyEnum<EnumShape> SHAPE = PropertyEnum.create("shape", EnumShape.class);

    protected final Block modelBlock;
    protected final IBlockState modelState;

    protected BlockStairs(IBlockState modelState) {
        super(modelState.getMaterial());
        this.modelBlock = modelState.getBlock();
        this.modelState = modelState;
        setDefaultState(blockState.getBaseState().withProperty(FACING, legacyconnector.v1_12_2.net.minecraft.util.EnumFacing.NORTH)
                .withProperty(HALF, EnumHalf.BOTTOM).withProperty(SHAPE, EnumShape.STRAIGHT));
        this.fullBlock = false;
        this.lightOpacity = 255;
    }

    @Override
    protected legacyconnector.v1_12_2.net.minecraft.block.state.BlockStateContainer createBlockState() {
        return new legacyconnector.v1_12_2.net.minecraft.block.state.BlockStateContainer(this, FACING, HALF, SHAPE);
    }

    /** Metadata as 1.12 stored it: bit 2 is the top half, the low two bits are 5 - the index of the facing (east, west, south, north). */
    @Override
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState().withProperty(HALF, (meta & 4) > 0 ? EnumHalf.TOP : EnumHalf.BOTTOM)
                .withProperty(FACING, legacyconnector.v1_12_2.net.minecraft.util.EnumFacing.byIndex(5 - (meta & 3)));
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        int meta = state.getValue(HALF) == EnumHalf.TOP ? 4 : 0;
        return meta | (5 - state.getValue(FACING).getIndex());
    }

    // 1.12's collision boxes: the slab half, the quarter the step stands on (straight and inner shapes) and the corner eighth (bent shapes)
    protected static final AxisAlignedBB AABB_SLAB_TOP = new AxisAlignedBB(0.0D, 0.5D, 0.0D, 1.0D, 1.0D, 1.0D);
    protected static final AxisAlignedBB AABB_QTR_TOP_WEST = new AxisAlignedBB(0.0D, 0.5D, 0.0D, 0.5D, 1.0D, 1.0D);
    protected static final AxisAlignedBB AABB_QTR_TOP_EAST = new AxisAlignedBB(0.5D, 0.5D, 0.0D, 1.0D, 1.0D, 1.0D);
    protected static final AxisAlignedBB AABB_QTR_TOP_NORTH = new AxisAlignedBB(0.0D, 0.5D, 0.0D, 1.0D, 1.0D, 0.5D);
    protected static final AxisAlignedBB AABB_QTR_TOP_SOUTH = new AxisAlignedBB(0.0D, 0.5D, 0.5D, 1.0D, 1.0D, 1.0D);
    protected static final AxisAlignedBB AABB_OCT_TOP_NW = new AxisAlignedBB(0.0D, 0.5D, 0.0D, 0.5D, 1.0D, 0.5D);
    protected static final AxisAlignedBB AABB_OCT_TOP_NE = new AxisAlignedBB(0.5D, 0.5D, 0.0D, 1.0D, 1.0D, 0.5D);
    protected static final AxisAlignedBB AABB_OCT_TOP_SW = new AxisAlignedBB(0.0D, 0.5D, 0.5D, 0.5D, 1.0D, 1.0D);
    protected static final AxisAlignedBB AABB_OCT_TOP_SE = new AxisAlignedBB(0.5D, 0.5D, 0.5D, 1.0D, 1.0D, 1.0D);
    protected static final AxisAlignedBB AABB_SLAB_BOTTOM = new AxisAlignedBB(0.0D, 0.0D, 0.0D, 1.0D, 0.5D, 1.0D);
    protected static final AxisAlignedBB AABB_QTR_BOT_WEST = new AxisAlignedBB(0.0D, 0.0D, 0.0D, 0.5D, 0.5D, 1.0D);
    protected static final AxisAlignedBB AABB_QTR_BOT_EAST = new AxisAlignedBB(0.5D, 0.0D, 0.0D, 1.0D, 0.5D, 1.0D);
    protected static final AxisAlignedBB AABB_QTR_BOT_NORTH = new AxisAlignedBB(0.0D, 0.0D, 0.0D, 1.0D, 0.5D, 0.5D);
    protected static final AxisAlignedBB AABB_QTR_BOT_SOUTH = new AxisAlignedBB(0.0D, 0.0D, 0.5D, 1.0D, 0.5D, 1.0D);
    protected static final AxisAlignedBB AABB_OCT_BOT_NW = new AxisAlignedBB(0.0D, 0.0D, 0.0D, 0.5D, 0.5D, 0.5D);
    protected static final AxisAlignedBB AABB_OCT_BOT_NE = new AxisAlignedBB(0.5D, 0.0D, 0.0D, 1.0D, 0.5D, 0.5D);
    protected static final AxisAlignedBB AABB_OCT_BOT_SW = new AxisAlignedBB(0.0D, 0.0D, 0.5D, 0.5D, 0.5D, 1.0D);
    protected static final AxisAlignedBB AABB_OCT_BOT_SE = new AxisAlignedBB(0.5D, 0.0D, 0.5D, 1.0D, 0.5D, 1.0D);

    /** The shape is not stored: 1.12 worked it out from the stairs behind and in front (corners), as here. */
    @Override
    public IBlockState getActualState(IBlockState state, IBlockAccess worldIn, BlockPos pos) {
        try {
            return state.withProperty(SHAPE, getStairsShape(state, worldIn, pos));
        } catch (RuntimeException e) {
            return state; // a neighbour state without the stairs properties: straight
        }
    }

    private static EnumShape getStairsShape(IBlockState state, IBlockAccess world, BlockPos pos) {
        EnumFacing facing = state.getValue(FACING);
        IBlockState back = world.getBlockState(pos.offset(facing));
        if (isBlockStairs(back) && state.getValue(HALF) == back.getValue(HALF)) {
            EnumFacing backFacing = back.getValue(FACING);
            if (backFacing.getAxis() != facing.getAxis() && isDifferentStairs(state, world, pos, backFacing.getOpposite())) {
                return backFacing == facing.rotateYCCW() ? EnumShape.OUTER_LEFT : EnumShape.OUTER_RIGHT;
            }
        }
        IBlockState front = world.getBlockState(pos.offset(facing.getOpposite()));
        if (isBlockStairs(front) && state.getValue(HALF) == front.getValue(HALF)) {
            EnumFacing frontFacing = front.getValue(FACING);
            if (frontFacing.getAxis() != facing.getAxis() && isDifferentStairs(state, world, pos, frontFacing)) {
                return frontFacing == facing.rotateYCCW() ? EnumShape.INNER_LEFT : EnumShape.INNER_RIGHT;
            }
        }
        return EnumShape.STRAIGHT;
    }

    private static boolean isDifferentStairs(IBlockState state, IBlockAccess world, BlockPos pos, EnumFacing side) {
        IBlockState other = world.getBlockState(pos.offset(side));
        return !isBlockStairs(other) || other.getValue(FACING) != state.getValue(FACING) || other.getValue(HALF) != state.getValue(HALF);
    }

    @Override
    public void addCollisionBoxToList(IBlockState state, World worldIn, BlockPos pos, AxisAlignedBB entityBox, List<AxisAlignedBB> collidingBoxes,
            Entity entityIn, boolean isActualState) {
        if (!isActualState) state = getActualState(state, worldIn, pos);
        for (AxisAlignedBB box : getCollisionBoxList(state)) addCollisionBoxToList(pos, entityBox, collidingBoxes, box);
    }

    private static List<AxisAlignedBB> getCollisionBoxList(IBlockState state) {
        List<AxisAlignedBB> list = new ArrayList<>(3);
        boolean top = state.getValue(HALF) == EnumHalf.TOP;
        list.add(top ? AABB_SLAB_TOP : AABB_SLAB_BOTTOM);
        EnumShape shape = state.getValue(SHAPE);
        if (shape == EnumShape.STRAIGHT || shape == EnumShape.INNER_LEFT || shape == EnumShape.INNER_RIGHT) list.add(getCollQuarterBlock(state));
        if (shape != EnumShape.STRAIGHT) list.add(getCollEighthBlock(state));
        return list;
    }

    private static AxisAlignedBB getCollQuarterBlock(IBlockState state) {
        boolean top = state.getValue(HALF) == EnumHalf.TOP;
        switch (state.getValue(FACING)) {
            case SOUTH:
                return top ? AABB_QTR_BOT_SOUTH : AABB_QTR_TOP_SOUTH;
            case WEST:
                return top ? AABB_QTR_BOT_WEST : AABB_QTR_TOP_WEST;
            case EAST:
                return top ? AABB_QTR_BOT_EAST : AABB_QTR_TOP_EAST;
            case NORTH:
            default:
                return top ? AABB_QTR_BOT_NORTH : AABB_QTR_TOP_NORTH;
        }
    }

    private static AxisAlignedBB getCollEighthBlock(IBlockState state) {
        EnumFacing facing = state.getValue(FACING);
        EnumFacing corner;
        switch (state.getValue(SHAPE)) {
            case OUTER_RIGHT:
                corner = facing.rotateY();
                break;
            case INNER_RIGHT:
                corner = facing.getOpposite();
                break;
            case INNER_LEFT:
                corner = facing.rotateYCCW();
                break;
            case OUTER_LEFT:
            default:
                corner = facing;
        }
        boolean top = state.getValue(HALF) == EnumHalf.TOP;
        switch (corner) {
            case SOUTH:
                return top ? AABB_OCT_BOT_SE : AABB_OCT_TOP_SE;
            case WEST:
                return top ? AABB_OCT_BOT_SW : AABB_OCT_TOP_SW;
            case EAST:
                return top ? AABB_OCT_BOT_NE : AABB_OCT_TOP_NE;
            case NORTH:
            default:
                return top ? AABB_OCT_BOT_NW : AABB_OCT_TOP_NW;
        }
    }

    /** Structures turn their stairs with them (Template, structure pieces): the facing turns. */
    @Override
    public IBlockState withRotation(IBlockState state, legacyconnector.v1_12_2.net.minecraft.util.Rotation rot) {
        return state.withProperty(FACING, rot.rotate(state.getValue(FACING)));
    }

    /** A mirrored stair faces back across the mirror plane; its bent shape swaps left and right (1.12's rule). */
    @Override
    public IBlockState withMirror(IBlockState state, legacyconnector.v1_12_2.net.minecraft.util.Mirror mirror) {
        EnumFacing facing = state.getValue(FACING);
        boolean across = mirror == legacyconnector.v1_12_2.net.minecraft.util.Mirror.LEFT_RIGHT && facing.getAxis() == EnumFacing.Axis.Z
                || mirror == legacyconnector.v1_12_2.net.minecraft.util.Mirror.FRONT_BACK && facing.getAxis() == EnumFacing.Axis.X;
        if (!across) return state;
        IBlockState turned = state.withRotation(legacyconnector.v1_12_2.net.minecraft.util.Rotation.CLOCKWISE_180);
        switch (state.getValue(SHAPE)) {
            case OUTER_LEFT:
                return turned.withProperty(SHAPE, EnumShape.OUTER_RIGHT);
            case OUTER_RIGHT:
                return turned.withProperty(SHAPE, EnumShape.OUTER_LEFT);
            case INNER_RIGHT:
                return mirror == legacyconnector.v1_12_2.net.minecraft.util.Mirror.LEFT_RIGHT ? turned.withProperty(SHAPE, EnumShape.INNER_LEFT) : turned;
            case INNER_LEFT:
                return mirror == legacyconnector.v1_12_2.net.minecraft.util.Mirror.LEFT_RIGHT ? turned.withProperty(SHAPE, EnumShape.INNER_RIGHT) : turned;
            default:
                return turned;
        }
    }

    /** The nearest hit on the stair's boxes (1.12 traced each collision box, not the full bounding box). */
    @Override
    public RayTraceResult collisionRayTrace(IBlockState blockState, World worldIn, BlockPos pos, Vec3d start, Vec3d end) {
        RayTraceResult best = null;
        double bestDistance = 0;
        for (AxisAlignedBB box : getCollisionBoxList(getActualState(blockState, worldIn, pos))) {
            RayTraceResult hit = rayTrace(pos, start, end, box);
            if (hit == null) continue;
            double dx = hit.hitVec.x - end.x, dy = hit.hitVec.y - end.y, dz = hit.hitVec.z - end.z;
            double distance = dx * dx + dy * dy + dz * dz;
            if (distance > bestDistance) {
                best = hit;
                bestDistance = distance;
            }
        }
        return best;
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return false;
    }

    /** Faces the placer, in the upper half when clicked under a block or on the upper half of a side. */
    @Override
    public IBlockState getStateForPlacement(legacyconnector.v1_12_2.net.minecraft.world.World world, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos,
            legacyconnector.v1_12_2.net.minecraft.util.EnumFacing facing, float hitX, float hitY, float hitZ, int meta,
            legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase placer) {
        IBlockState state = super.getStateForPlacement(world, pos, facing, hitX, hitY, hitZ, meta, placer).withProperty(SHAPE, EnumShape.STRAIGHT);
        legacyconnector.v1_12_2.net.minecraft.util.EnumFacing look = placer == null ? legacyconnector.v1_12_2.net.minecraft.util.EnumFacing.NORTH : placer.getHorizontalFacing();
        boolean top = facing != legacyconnector.v1_12_2.net.minecraft.util.EnumFacing.UP && (facing == legacyconnector.v1_12_2.net.minecraft.util.EnumFacing.DOWN || hitY > 0.5F);
        return state.withProperty(FACING, look).withProperty(HALF, top ? EnumHalf.TOP : EnumHalf.BOTTOM);
    }

    public enum EnumHalf implements IStringSerializable {
        TOP("top"), BOTTOM("bottom");

        private final String name;

        EnumHalf(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public enum EnumShape implements IStringSerializable {
        STRAIGHT("straight"), INNER_LEFT("inner_left"), INNER_RIGHT("inner_right"), OUTER_LEFT("outer_left"), OUTER_RIGHT("outer_right");

        private final String name;

        EnumShape(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    /** Whether the state is of a stairs block. */
    public static boolean isBlockStairs(legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state) {
        return state.getBlock() instanceof BlockStairs;
    }
}
