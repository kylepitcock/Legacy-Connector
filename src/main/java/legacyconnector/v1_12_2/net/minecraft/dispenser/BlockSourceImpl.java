package legacyconnector.v1_12_2.net.minecraft.dispenser;

import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.tileentity.TileEntity;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/** A dispensing block in a world; {@link #mcc$of} views a modern dispenser's source. */
public class BlockSourceImpl implements IBlockSource {
    private final World world;
    private final BlockPos pos;

    public BlockSourceImpl(World worldIn, BlockPos posIn) {
        this.world = worldIn;
        this.pos = posIn;
    }

    public static BlockSourceImpl mcc$of(net.minecraft.core.dispenser.BlockSource source) {
        var p = source.pos();
        return new BlockSourceImpl(org.kylepitcock.legacyconnector.v1_12_2.bridge.WorldBridge1122.wrap(source.level()), new BlockPos(p.getX(), p.getY(), p.getZ()));
    }

    @Override
    public World getWorld() {
        return world;
    }

    @Override
    public double getX() {
        return pos.getX() + 0.5D;
    }

    @Override
    public double getY() {
        return pos.getY() + 0.5D;
    }

    @Override
    public double getZ() {
        return pos.getZ() + 0.5D;
    }

    @Override
    public BlockPos getBlockPos() {
        return pos;
    }

    @Override
    public IBlockState getBlockState() {
        return world.getBlockState(pos);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends TileEntity> T getBlockTileEntity() {
        return (T) world.getTileEntity(pos);
    }

    /** The direction the dispenser faces. */
    public legacyconnector.v1_12_2.net.minecraft.util.EnumFacing mcc$facing() {
        var state = world.mcc$level().getBlockState(new net.minecraft.core.BlockPos(pos.getX(), pos.getY(), pos.getZ()));
        return state.hasProperty(net.minecraft.world.level.block.DispenserBlock.FACING)
                ? legacyconnector.v1_12_2.net.minecraft.util.EnumFacing.mcc$of(state.getValue(net.minecraft.world.level.block.DispenserBlock.FACING))
                : legacyconnector.v1_12_2.net.minecraft.util.EnumFacing.NORTH;
    }

    /** The point items leave the dispenser from (0.7 blocks out of its front face). */
    public IPosition mcc$dispensePosition() {
        var f = mcc$facing();
        return new PositionImpl(getX() + 0.7D * f.getXOffset(), getY() + 0.7D * f.getYOffset(), getZ() + 0.7D * f.getZOffset());
    }
}
