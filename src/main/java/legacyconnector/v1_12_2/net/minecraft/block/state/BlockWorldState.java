package legacyconnector.v1_12_2.net.minecraft.block.state;

import com.google.common.base.Predicate;
import legacyconnector.v1_12_2.net.minecraft.tileentity.TileEntity;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/** A position in a world whose state and tile entity are read once, on first use (block patterns test many of them). */
public class BlockWorldState {
    private final World world;
    private final BlockPos pos;
    private final boolean forceLoad;
    private IBlockState state;
    private TileEntity tileEntity;
    private boolean tileEntityInitialized;

    public BlockWorldState(World worldIn, BlockPos posIn, boolean forceLoadIn) {
        this.world = worldIn;
        this.pos = posIn;
        this.forceLoad = forceLoadIn;
    }

    public IBlockState getBlockState() {
        if (state == null && (forceLoad || world.isBlockLoaded(pos))) state = world.getBlockState(pos);
        return state;
    }

    public TileEntity getTileEntity() {
        if (tileEntity == null && !tileEntityInitialized) {
            tileEntity = world.getTileEntity(pos);
            tileEntityInitialized = true;
        }
        return tileEntity;
    }

    public BlockPos getPos() {
        return pos;
    }

    /** A test of the position's state. */
    public static Predicate<BlockWorldState> hasState(final Predicate<IBlockState> predicatesIn) {
        return input -> input != null && predicatesIn.apply(input.getBlockState());
    }
}
