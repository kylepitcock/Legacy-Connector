package legacyconnector.v1_12_2.net.minecraft.world;

import java.util.Random;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.tileentity.TileEntity;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.StatesBridge1122;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.TileEntityBridge1122;

/** 1.12.2 world: a view over a modern level. */
public class World {
    private final net.minecraft.world.level.Level level;

    public final boolean isRemote;
    public final Random rand = new Random();

    protected World(net.minecraft.world.level.Level level) {
        this.level = level;
        this.isRemote = level != null && level.isClientSide();
    }

    public net.minecraft.world.level.Level mcc$level() {
        return level;
    }

    private static net.minecraft.core.BlockPos modern(BlockPos p) {
        return new net.minecraft.core.BlockPos(p.getX(), p.getY(), p.getZ());
    }

    public IBlockState getBlockState(BlockPos pos) {
        IBlockState state = StatesBridge1122.toLegacy(level.getBlockState(modern(pos)));
        if (state == null) state = org.kylepitcock.legacyconnector.v1_12_2.bridge.Vanilla1122.airState();
        return state;
    }

    public boolean setBlockState(BlockPos pos, IBlockState state) {
        return setBlockState(pos, state, 3);
    }

    public boolean setBlockState(BlockPos pos, IBlockState state, int flags) {
        return level.setBlock(modern(pos), StatesBridge1122.toModern(state), flags);
    }

    public boolean isAirBlock(BlockPos pos) {
        return level.getBlockState(modern(pos)).isAir();
    }

    public boolean isBlockLoaded(BlockPos pos) {
        return level.hasChunkAt(modern(pos));
    }

    public TileEntity getTileEntity(BlockPos pos) {
        return TileEntityBridge1122.legacyAt(level, modern(pos));
    }

    public void markChunkDirty(BlockPos pos, TileEntity te) {
        level.getChunkAt(modern(pos)).setUnsaved(true);
    }

    public void notifyBlockUpdate(BlockPos pos, IBlockState oldState, IBlockState newState, int flags) {
        level.sendBlockUpdated(modern(pos), StatesBridge1122.toModern(oldState), StatesBridge1122.toModern(newState), flags);
    }

    public long getTotalWorldTime() {
        return level.getGameTime();
    }

    public long getWorldTime() {
        return level.getDayTime();
    }

    public boolean isDaytime() {
        return level.isDay();
    }
}
