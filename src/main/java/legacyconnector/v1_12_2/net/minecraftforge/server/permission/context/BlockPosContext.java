package legacyconnector.v1_12_2.net.minecraftforge.server.permission.context;

import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;

/** A player acting on a block: its position, state (read from the world when not given) and side. */
public class BlockPosContext extends PlayerContext {
    private final BlockPos blockPos;
    private IBlockState blockState;
    private final EnumFacing facing;

    public BlockPosContext(EntityPlayer ep, BlockPos pos, IBlockState state, EnumFacing f) {
        super(ep);
        this.blockPos = pos;
        this.blockState = state;
        this.facing = f;
    }

    public BlockPosContext(EntityPlayer ep, legacyconnector.v1_12_2.net.minecraft.util.math.ChunkPos pos) {
        this(ep, new BlockPos(pos.getXStart() + 8, 0, pos.getZStart() + 8), null, null);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(ContextKey<T> key) {
        if (key.equals(ContextKeys.POS)) return (T) blockPos;
        if (key.equals(ContextKeys.BLOCK_STATE)) {
            if (blockState == null && getWorld() != null) blockState = getWorld().getBlockState(blockPos);
            return (T) blockState;
        }
        if (key.equals(ContextKeys.FACING)) return (T) facing;
        return super.get(key);
    }

    @Override
    protected boolean covers(ContextKey<?> key) {
        return key.equals(ContextKeys.POS) || key.equals(ContextKeys.BLOCK_STATE) || facing != null && key.equals(ContextKeys.FACING);
    }

    @Override
    public boolean has(ContextKey<?> key) {
        return covers(key) || super.has(key);
    }
}
