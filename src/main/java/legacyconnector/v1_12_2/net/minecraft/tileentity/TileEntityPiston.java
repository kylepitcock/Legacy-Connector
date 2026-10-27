package legacyconnector.v1_12_2.net.minecraft.tileentity;

import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import legacyconnector.v1_12_2.net.minecraft.util.ITickable;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.StatesBridge1122;

/**
 * 1.12.2's moving block of a piston: the block moved, the direction, whether it extends and the progress. A moving block of the game is
 * seen through a view of it (renderers of mods' blocks and movement checks read it); a legacy one advances by half a block a tick.
 */
public class TileEntityPiston extends TileEntity implements ITickable {
    private IBlockState pistonState;
    private EnumFacing pistonFacing;
    private boolean extending;
    private boolean shouldHeadBeRendered;
    private float progress;
    private float lastProgress;
    private final net.minecraft.world.level.block.piston.PistonMovingBlockEntity mcc$moving;

    public TileEntityPiston() {
        this.mcc$moving = null;
    }

    public TileEntityPiston(IBlockState pistonStateIn, EnumFacing pistonFacingIn, boolean extendingIn, boolean shouldHeadBeRenderedIn) {
        this.mcc$moving = null;
        this.pistonState = pistonStateIn;
        this.pistonFacing = pistonFacingIn;
        this.extending = extendingIn;
        this.shouldHeadBeRendered = shouldHeadBeRenderedIn;
    }

    /** A view of a game moving block. */
    public TileEntityPiston(net.minecraft.world.level.block.piston.PistonMovingBlockEntity moving) {
        this.mcc$moving = moving;
    }

    public IBlockState getPistonState() {
        return mcc$moving != null ? StatesBridge1122.toLegacy(mcc$moving.getMovedState()) : pistonState;
    }

    public boolean isExtending() {
        return mcc$moving != null ? mcc$moving.isExtending() : extending;
    }

    public EnumFacing getFacing() {
        return mcc$moving != null ? EnumFacing.byIndex(mcc$moving.getDirection().get3DDataValue()) : pistonFacing;
    }

    public boolean shouldPistonHeadBeRendered() {
        return mcc$moving != null ? mcc$moving.isSourcePiston() : shouldHeadBeRendered;
    }

    public float getProgress(float ticks) {
        if (mcc$moving != null) return mcc$moving.getProgress(ticks);
        if (ticks > 1.0F) ticks = 1.0F;
        return lastProgress + (progress - lastProgress) * ticks;
    }

    private float getExtendedProgress(float p) {
        return extending ? p - 1.0F : 1.0F - p;
    }

    public float getOffsetX(float ticks) {
        if (mcc$moving != null) return mcc$moving.getXOff(ticks);
        return (float) pistonFacing.getXOffset() * getExtendedProgress(getProgress(ticks));
    }

    public float getOffsetY(float ticks) {
        if (mcc$moving != null) return mcc$moving.getYOff(ticks);
        return (float) pistonFacing.getYOffset() * getExtendedProgress(getProgress(ticks));
    }

    public float getOffsetZ(float ticks) {
        if (mcc$moving != null) return mcc$moving.getZOff(ticks);
        return (float) pistonFacing.getZOffset() * getExtendedProgress(getProgress(ticks));
    }

    /** Finishes the move: the moved block is put in place. */
    public void clearPistonTileEntity() {
        if (mcc$moving != null) {
            mcc$moving.finalTick();
            return;
        }
        if (lastProgress < 1.0F && world != null) {
            progress = 1.0F;
            lastProgress = progress;
            world.removeTileEntity(pos);
            invalidate();
            if (pistonState != null) world.setBlockState(pos, pistonState, 3);
        }
    }

    @Override
    public void update() {
        if (mcc$moving != null) return;
        lastProgress = progress;
        if (lastProgress >= 1.0F) {
            if (world != null && pistonState != null) {
                world.removeTileEntity(pos);
                invalidate();
                world.setBlockState(pos, pistonState, 3);
            }
        } else {
            progress = Math.min(1.0F, progress + 0.5F);
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        pistonFacing = EnumFacing.byIndex(compound.getInteger("facing"));
        progress = compound.getFloat("progress");
        lastProgress = progress;
        extending = compound.getBoolean("extending");
        shouldHeadBeRendered = compound.getBoolean("source");
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        if (pistonFacing != null) compound.setInteger("facing", pistonFacing.getIndex());
        compound.setFloat("progress", lastProgress);
        compound.setBoolean("extending", extending);
        compound.setBoolean("source", shouldHeadBeRendered);
        return compound;
    }
}
