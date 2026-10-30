package legacyconnector.v1_12_2.net.minecraft.block;

import java.util.Random;
import legacyconnector.v1_12_2.net.minecraft.block.material.Material;
import legacyconnector.v1_12_2.net.minecraft.block.properties.PropertyEnum;
import legacyconnector.v1_12_2.net.minecraft.block.state.BlockFaceShape;
import legacyconnector.v1_12_2.net.minecraft.block.state.BlockStateContainer;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.util.BlockRenderLayer;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import legacyconnector.v1_12_2.net.minecraft.util.Rotation;
import legacyconnector.v1_12_2.net.minecraft.util.math.AxisAlignedBB;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.IBlockAccess;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/**
 * 1.12.2 nether portal: portal blocks along the X or Z axis (metadata 1 or 2) inside an obsidian frame; {@link Size} measures and fills
 * a frame (mods subclass it for their own frames). Travelling is the modern portal's.
 */
public class BlockPortal extends BlockBreakable {
    public static final PropertyEnum<EnumFacing.Axis> AXIS = PropertyEnum.create("axis", EnumFacing.Axis.class, EnumFacing.Axis.X, EnumFacing.Axis.Z);
    protected static final AxisAlignedBB X_AABB = new AxisAlignedBB(0.0D, 0.0D, 0.375D, 1.0D, 1.0D, 0.625D);
    protected static final AxisAlignedBB Z_AABB = new AxisAlignedBB(0.375D, 0.0D, 0.0D, 0.625D, 1.0D, 1.0D);
    protected static final AxisAlignedBB Y_AABB = new AxisAlignedBB(0.375D, 0.0D, 0.375D, 0.625D, 1.0D, 0.625D);

    public BlockPortal() {
        super(Material.PORTAL, false);
        setDefaultState(blockState.getBaseState().withProperty(AXIS, EnumFacing.Axis.X));
        setTickRandomly(true);
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        return switch (state.getValue(AXIS)) {
            case X -> X_AABB;
            case Z -> Z_AABB;
            default -> Y_AABB;
        };
    }

    @Override
    public AxisAlignedBB getCollisionBoundingBox(IBlockState blockState, IBlockAccess worldIn, BlockPos pos) {
        return NULL_AABB;
    }

    public static int getMetaForAxis(EnumFacing.Axis axis) {
        return axis == EnumFacing.Axis.X ? 1 : axis == EnumFacing.Axis.Z ? 2 : 0;
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return false;
    }

    /** Lights a portal in an obsidian frame around the position; true when one was made. */
    public boolean trySpawnPortal(World worldIn, BlockPos pos) {
        Size x = new Size(worldIn, pos, EnumFacing.Axis.X);
        if (x.isValid() && x.portalBlockCount == 0) {
            x.placePortalBlocks();
            return true;
        }
        Size z = new Size(worldIn, pos, EnumFacing.Axis.Z);
        if (z.isValid() && z.portalBlockCount == 0) {
            z.placePortalBlocks();
            return true;
        }
        return false;
    }

    /** The portal breaks when its frame does. */
    @Override
    public void neighborChanged(IBlockState state, World worldIn, BlockPos pos, Block blockIn, BlockPos fromPos) {
        EnumFacing.Axis axis = state.getValue(AXIS);
        if (axis == EnumFacing.Axis.X) {
            Size size = new Size(worldIn, pos, EnumFacing.Axis.X);
            if (!size.isValid() || size.portalBlockCount < size.width * size.height) worldIn.setBlockState(pos, legacyconnector.v1_12_2.net.minecraft.init.Blocks.AIR.getDefaultState());
        } else if (axis == EnumFacing.Axis.Z) {
            Size size = new Size(worldIn, pos, EnumFacing.Axis.Z);
            if (!size.isValid() || size.portalBlockCount < size.width * size.height) worldIn.setBlockState(pos, legacyconnector.v1_12_2.net.minecraft.init.Blocks.AIR.getDefaultState());
        }
    }

    @Override
    public int quantityDropped(Random random) {
        return 0;
    }

    @Override
    public BlockRenderLayer getRenderLayer() {
        return BlockRenderLayer.TRANSLUCENT;
    }

    @Override
    public void onEntityCollision(World worldIn, BlockPos pos, IBlockState state, Entity entityIn) {
        if (!entityIn.isRiding() && !entityIn.isBeingRidden() && entityIn.isNonBoss()) entityIn.setPortal(pos);
    }

    @Override
    public ItemStack getItem(World worldIn, BlockPos pos, IBlockState state) {
        return ItemStack.EMPTY;
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState().withProperty(AXIS, (meta & 3) == 2 ? EnumFacing.Axis.Z : EnumFacing.Axis.X);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return getMetaForAxis(state.getValue(AXIS));
    }

    @Override
    public IBlockState withRotation(IBlockState state, Rotation rot) {
        if (rot == Rotation.COUNTERCLOCKWISE_90 || rot == Rotation.CLOCKWISE_90) {
            EnumFacing.Axis axis = state.getValue(AXIS);
            if (axis == EnumFacing.Axis.X) return state.withProperty(AXIS, EnumFacing.Axis.Z);
            if (axis == EnumFacing.Axis.Z) return state.withProperty(AXIS, EnumFacing.Axis.X);
        }
        return state;
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, AXIS);
    }

    @Override
    public BlockFaceShape getBlockFaceShape(IBlockAccess worldIn, IBlockState state, BlockPos pos, EnumFacing face) {
        return BlockFaceShape.UNDEFINED;
    }

    /** A portal frame along an axis: its inside size (2-21 wide, 3-21 high), its bottom-left corner and how many portal blocks fill it. */
    public static class Size {
        protected final World world;
        protected final EnumFacing.Axis axis;
        protected final EnumFacing rightDir;
        protected final EnumFacing leftDir;
        protected int portalBlockCount;
        protected BlockPos bottomLeft;
        protected int height;
        protected int width;

        public Size(World worldIn, BlockPos p_i45694_2_, EnumFacing.Axis p_i45694_3_) {
            this.world = worldIn;
            this.axis = p_i45694_3_;
            if (p_i45694_3_ == EnumFacing.Axis.X) {
                this.leftDir = EnumFacing.EAST;
                this.rightDir = EnumFacing.WEST;
            } else {
                this.leftDir = EnumFacing.NORTH;
                this.rightDir = EnumFacing.SOUTH;
            }
            for (BlockPos start = p_i45694_2_; p_i45694_2_.getY() > start.getY() - 21 && p_i45694_2_.getY() > 0
                    && isEmptyBlock(worldIn.getBlockState(p_i45694_2_.down()).getBlock()); p_i45694_2_ = p_i45694_2_.down()) {
            }
            int i = getDistanceUntilEdge(p_i45694_2_, leftDir) - 1;
            if (i >= 0) {
                this.bottomLeft = p_i45694_2_.offset(leftDir, i);
                this.width = getDistanceUntilEdge(bottomLeft, rightDir);
                if (width < 2 || width > 21) {
                    this.bottomLeft = null;
                    this.width = 0;
                }
            }
            if (bottomLeft != null) this.height = calculatePortalHeight();
        }

        protected int getDistanceUntilEdge(BlockPos p_180120_1_, EnumFacing p_180120_2_) {
            int i;
            for (i = 0; i < 22; ++i) {
                BlockPos p = p_180120_1_.offset(p_180120_2_, i);
                if (!isEmptyBlock(world.getBlockState(p).getBlock()) || !isFrame(world.getBlockState(p.down()).getBlock())) break;
            }
            Block edge = world.getBlockState(p_180120_1_.offset(p_180120_2_, i)).getBlock();
            return isFrame(edge) ? i : 0;
        }

        public int getHeight() {
            return height;
        }

        public int getWidth() {
            return width;
        }

        protected int calculatePortalHeight() {
            label:
            for (height = 0; height < 21; ++height) {
                for (int i = 0; i < width; ++i) {
                    BlockPos p = bottomLeft.offset(rightDir, i).up(height);
                    Block block = world.getBlockState(p).getBlock();
                    if (!isEmptyBlock(block)) break label;
                    if (block instanceof BlockPortal || Block.mcc$named(block, "portal")) ++portalBlockCount;
                    if (i == 0) {
                        if (!isFrame(world.getBlockState(p.offset(leftDir)).getBlock())) break label;
                    } else if (i == width - 1) {
                        if (!isFrame(world.getBlockState(p.offset(rightDir)).getBlock())) break label;
                    }
                }
            }
            for (int j = 0; j < width; ++j) {
                if (!isFrame(world.getBlockState(bottomLeft.offset(rightDir, j).up(height)).getBlock())) {
                    height = 0;
                    break;
                }
            }
            if (height <= 21 && height >= 3) return height;
            bottomLeft = null;
            width = 0;
            height = 0;
            return 0;
        }

        /** Air, fire or portal: what may be inside a frame. */
        protected boolean isEmptyBlock(Block blockIn) {
            return blockIn.getMaterialLegacy() == Material.AIR || Block.mcc$named(blockIn, "fire", "portal") || blockIn instanceof BlockPortal
                    || blockIn instanceof BlockFire;
        }

        private static boolean isFrame(Block block) {
            return Block.mcc$named(block, "obsidian");
        }

        public boolean isValid() {
            return bottomLeft != null && width >= 2 && width <= 21 && height >= 3 && height <= 21;
        }

        /** Fills the frame with portal blocks of its axis. */
        public void placePortalBlocks() {
            for (int i = 0; i < width; ++i) {
                BlockPos p = bottomLeft.offset(rightDir, i);
                for (int j = 0; j < height; ++j) {
                    world.setBlockState(p.up(j), legacyconnector.v1_12_2.net.minecraft.init.Blocks.PORTAL.getDefaultState().withProperty(BlockPortal.AXIS, axis), 2);
                }
            }
        }
    }
}
