package legacyconnector.v1_12_2.net.minecraft.block;

import legacyconnector.v1_12_2.net.minecraft.block.material.Material;
import legacyconnector.v1_12_2.net.minecraft.block.properties.PropertyEnum;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import legacyconnector.v1_12_2.net.minecraft.util.IStringSerializable;
import legacyconnector.v1_12_2.net.minecraft.util.Rotation;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/** A log: wood, placed along the clicked side's axis (LOG_AXIS; "none" is the bark-all-over block). Subclasses add the wood type. */
public abstract class BlockLog extends BlockRotatedPillar {
    public static final PropertyEnum<EnumAxis> LOG_AXIS = PropertyEnum.create("axis", EnumAxis.class);

    public BlockLog() {
        super(Material.WOOD);
        setCreativeTab(legacyconnector.v1_12_2.net.minecraft.creativetab.CreativeTabs.BUILDING_BLOCKS);
        setHardness(2.0F);
        setSoundType(SoundType.WOOD);
    }

    /** Legacy leaves around start their decay check (the modern leaves decay by their distance to logs on their own). */
    @Override
    public void breakBlock(World world, BlockPos pos, IBlockState state) {
        if (!world.isAreaLoaded(pos.add(-5, -5, -5), pos.add(5, 5, 5))) return;
        for (int x = -4; x <= 4; x++) {
            for (int y = -4; y <= 4; y++) {
                for (int z = -4; z <= 4; z++) {
                    BlockPos p = pos.add(x, y, z);
                    IBlockState s = world.getBlockState(p);
                    if (s.getBlock() instanceof BlockLeaves && s.getBlock().isLeaves(s, world, p)) s.getBlock().beginLeavesDecay(s, world, p);
                }
            }
        }
    }

    @Override
    public boolean canSustainLeaves(IBlockState state, legacyconnector.v1_12_2.net.minecraft.world.IBlockAccess world, BlockPos pos) {
        return true;
    }

    @Override
    public boolean isWood(legacyconnector.v1_12_2.net.minecraft.world.IBlockAccess world, BlockPos pos) {
        return true;
    }

    @Override
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
        return getStateFromMeta(meta).withProperty(LOG_AXIS, EnumAxis.fromFacingAxis(facing.getAxis()));
    }

    @Override
    public IBlockState withRotation(IBlockState state, Rotation rot) {
        if (rot != Rotation.COUNTERCLOCKWISE_90 && rot != Rotation.CLOCKWISE_90) return state;
        return switch (state.getValue(LOG_AXIS)) {
            case X -> state.withProperty(LOG_AXIS, EnumAxis.Z);
            case Z -> state.withProperty(LOG_AXIS, EnumAxis.X);
            default -> state;
        };
    }

    public enum EnumAxis implements IStringSerializable {
        X("x"), Y("y"), Z("z"), NONE("none");

        private final String name;

        EnumAxis(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }

        public static EnumAxis fromFacingAxis(EnumFacing.Axis axis) {
            return switch (axis) {
                case X -> X;
                case Y -> Y;
                case Z -> Z;
            };
        }

        @Override
        public String getName() {
            return name;
        }
    }
}
