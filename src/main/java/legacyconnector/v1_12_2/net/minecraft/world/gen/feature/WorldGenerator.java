package legacyconnector.v1_12_2.net.minecraft.world.gen.feature;

import java.util.Random;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/** A 1.12.2 feature generator: places something at a position. */
public abstract class WorldGenerator {
    private final boolean doBlockNotify;

    public WorldGenerator() {
        this(false);
    }

    public WorldGenerator(boolean notify) {
        this.doBlockNotify = notify;
    }

    public abstract boolean generate(World worldIn, Random rand, BlockPos position);

    public void setDecorationDefaults() {
    }

    /** With notification (saplings) a normal update; during generation no neighbour reactions (Forge's cascading fix). */
    protected void setBlockAndNotifyAdequately(World worldIn, BlockPos pos, IBlockState state) {
        if (doBlockNotify) worldIn.setBlockState(pos, state, 3);
        else worldIn.setBlockState(pos, state, 2 | 16);
    }

    /** Sets a modern vanilla state (the vanilla blocks legacy wrappers cannot express with their properties). */
    protected boolean mcc$setModern(World worldIn, BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
        return worldIn.mcc$setModernState(pos, state, doBlockNotify ? 3 : 2 | 16);
    }

    protected net.minecraft.world.level.block.state.BlockState mcc$modernAt(World worldIn, BlockPos pos) {
        return worldIn.mcc$state(new net.minecraft.core.BlockPos(pos.getX(), pos.getY(), pos.getZ()));
    }
}
