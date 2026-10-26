package legacyconnector.v1_7_10.net.minecraft.tileentity;

import legacyconnector.v1_7_10.net.minecraft.block.Block;
import legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.level.block.piston.PistonMovingBlockEntity;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.BlockBridge;

/**
 * A block being moved by a piston: the block and metadata it carries, the direction (0-5) and whether it is extending. A view of
 * a modern moving piston reads the modern block entity; one legacy code creates ({@code BlockPistonMoving.getTileEntity}) becomes a
 * modern moving piston when it is set into the world.
 */
public class TileEntityPiston extends TileEntity {
    private Block storedBlock;
    private int storedMetadata;
    private int storedOrientation;
    private boolean extending;
    private boolean shouldHeadBeRendered;
    private float progress;
    private float lastProgress;
    private final PistonMovingBlockEntity mcc$moving;

    public TileEntityPiston() {
        this.mcc$moving = null;
    }

    public TileEntityPiston(Block block, int meta, int orientation, boolean extending, boolean renderHead) {
        this.storedBlock = block;
        this.storedMetadata = meta;
        this.storedOrientation = orientation;
        this.extending = extending;
        this.shouldHeadBeRendered = renderHead;
        this.mcc$moving = null;
    }

    /** View of a modern moving piston. */
    public TileEntityPiston(PistonMovingBlockEntity moving) {
        this.mcc$moving = moving;
    }

    public Block getStoredBlockID() {
        return mcc$moving != null ? BlockBridge.legacyBlock(mcc$moving.getMovedState()) : storedBlock;
    }

    @Override
    public int getBlockMetadata() {
        return mcc$moving != null ? BlockBridge.legacyMeta(mcc$moving.getMovedState()) : storedMetadata;
    }

    public boolean isExtending() {
        return mcc$moving != null ? mcc$moving.isExtending() : extending;
    }

    /** The direction the block moves in (0 down, 1 up, 2 north, 3 south, 4 west, 5 east). */
    public int getPistonOrientation() {
        return mcc$moving != null ? mcc$moving.getDirection().get3DDataValue() : storedOrientation;
    }

    public boolean shouldPistonHeadBeRendered() {
        return mcc$moving != null ? mcc$moving.isSourcePiston() : shouldHeadBeRendered;
    }

    /** {@code func_145860_a}: the move's progress (0-1) at the partial tick. */
    public float func_145860_a(float partialTicks) {
        if (mcc$moving != null) return mcc$moving.getProgress(partialTicks);
        if (partialTicks > 1.0F) partialTicks = 1.0F;
        return lastProgress + (progress - lastProgress) * partialTicks;
    }

    private float mcc$offset(float partialTicks, int axis) {
        if (mcc$moving != null) {
            return switch (axis) {
                case 0 -> mcc$moving.getXOff(partialTicks);
                case 1 -> mcc$moving.getYOff(partialTicks);
                default -> mcc$moving.getZOff(partialTicks);
            };
        }
        int[] offsets = switch (storedOrientation) {
            case 0 -> new int[] {0, -1, 0};
            case 1 -> new int[] {0, 1, 0};
            case 2 -> new int[] {0, 0, -1};
            case 3 -> new int[] {0, 0, 1};
            case 4 -> new int[] {-1, 0, 0};
            default -> new int[] {1, 0, 0};
        };
        float p = func_145860_a(partialTicks);
        return extending ? (p - 1.0F) * offsets[axis] : (1.0F - p) * offsets[axis];
    }

    /** {@code func_145865_b}, {@code func_145862_c}, {@code func_145859_d}: the drawn x, y, z offset of the moving block. */
    public float func_145865_b(float partialTicks) {
        return mcc$offset(partialTicks, 0);
    }

    public float func_145862_c(float partialTicks) {
        return mcc$offset(partialTicks, 1);
    }

    public float func_145859_d(float partialTicks) {
        return mcc$offset(partialTicks, 2);
    }

    /** Ends the move at once: the carried block is placed (a modern moving piston finishes itself). */
    public void clearPistonTileEntity() {
        if (mcc$moving != null) {
            mcc$moving.finalTick();
            return;
        }
        if (lastProgress < 1.0F && worldObj != null) {
            lastProgress = progress = 1.0F;
            worldObj.removeTileEntity(xCoord, yCoord, zCoord);
            invalidate();
            if (worldObj.getBlock(xCoord, yCoord, zCoord) == legacyconnector.v1_7_10.net.minecraft.init.Blocks.piston_extension && storedBlock != null) {
                worldObj.setBlock(xCoord, yCoord, zCoord, storedBlock, storedMetadata, 3);
                worldObj.notifyBlockOfNeighborChange(xCoord, yCoord, zCoord, storedBlock);
            }
        }
    }

    @Override
    public void updateEntity() {
        if (mcc$moving != null) return;
        lastProgress = progress;
        if (lastProgress >= 1.0F) {
            clearPistonTileEntity();
        } else {
            progress += 0.5F;
            if (progress >= 1.0F) progress = 1.0F;
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        storedBlock = Block.getBlockById(tag.getInteger("blockId"));
        storedMetadata = tag.getInteger("blockData");
        storedOrientation = tag.getInteger("facing");
        lastProgress = progress = tag.getFloat("progress");
        extending = tag.getBoolean("extending");
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        tag.setInteger("blockId", Block.getIdFromBlock(getStoredBlockID()));
        tag.setInteger("blockData", getBlockMetadata());
        tag.setInteger("facing", getPistonOrientation());
        tag.setFloat("progress", lastProgress);
        tag.setBoolean("extending", isExtending());
    }

    /** The modern moving piston this legacy one becomes when legacy code puts it at a moving-piston block. */
    public net.minecraft.world.level.block.entity.BlockEntity mcc$toModern(net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
        if (mcc$moving != null) return mcc$moving;
        if (storedBlock == null) return null;
        var moved = BlockBridge.toModern(storedBlock, storedMetadata);
        var direction = net.minecraft.core.Direction.from3DDataValue(storedOrientation);
        return net.minecraft.world.level.block.piston.MovingPistonBlock.newMovingBlockEntity(pos, state, moved, direction, extending, shouldHeadBeRendered);
    }
}
