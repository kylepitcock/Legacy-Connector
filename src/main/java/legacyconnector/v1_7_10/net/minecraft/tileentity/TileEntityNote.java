package legacyconnector.v1_7_10.net.minecraft.tileentity;

import legacyconnector.v1_7_10.net.minecraft.block.material.Material;
import legacyconnector.v1_7_10.net.minecraft.init.Blocks;
import legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/**
 * A note block's pitch (0-24) and last redstone state. The modern note block keeps its note in the block state, so for a
 * vanilla note block this is a view: {@link #note} is read from the state when the view is handed out and written back on
 * {@link #markDirty()}.
 */
public class TileEntityNote extends TileEntity {
    public byte note;
    public boolean previousRedstoneState;
    /** The modern note block this views; null for a standalone tile entity. */
    private final net.minecraft.world.level.Level mcc$level;
    private final net.minecraft.core.BlockPos mcc$pos;
    /** The note read from the block: the block is only changed when a mod changed the note since. */
    private byte mcc$seen;

    public TileEntityNote() {
        this.mcc$level = null;
        this.mcc$pos = null;
    }

    /** View of the vanilla note block at a position. */
    public TileEntityNote(net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos) {
        this.mcc$level = level;
        this.mcc$pos = pos.immutable();
        var state = level.getBlockState(pos);
        if (state.hasProperty(net.minecraft.world.level.block.NoteBlock.NOTE)) note = (byte) (int) state.getValue(net.minecraft.world.level.block.NoteBlock.NOTE);
        mcc$seen = note;
        if (state.hasProperty(net.minecraft.world.level.block.NoteBlock.POWERED)) previousRedstoneState = state.getValue(net.minecraft.world.level.block.NoteBlock.POWERED);
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        tag.setByte("note", note);
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        note = tag.getByte("note");
        if (note < 0) note = 0;
        if (note > 24) note = 24;
    }

    @Override
    public void markDirty() {
        if (mcc$level != null && note != mcc$seen) {
            mcc$seen = note;
            var state = mcc$level.getBlockState(mcc$pos);
            var key = net.minecraft.world.level.block.NoteBlock.NOTE;
            int want = Math.max(0, Math.min(24, note));
            if (state.hasProperty(key) && state.getValue(key) != want) mcc$level.setBlock(mcc$pos, state.setValue(key, want), 3);
        }
        super.markDirty();
    }

    /** One step up in pitch (wrapping after 24). */
    public void changePitch() {
        note = (byte) ((note + 1) % 25);
        markDirty();
    }

    /** Plays the note when the block above is air (the instrument comes from the block below). */
    public void triggerNote(World world, int x, int y, int z) {
        if (world.getBlock(x, y + 1, z).getMaterial() != Material.air) return;
        if (mcc$level != null) {
            mcc$level.blockEvent(mcc$pos, mcc$level.getBlockState(mcc$pos).getBlock(), 0, 0);
            return;
        }
        Material below = world.getBlock(x, y - 1, z).getMaterial();
        int instrument = 0;
        if (below == Material.rock) instrument = 1;
        if (below == Material.sand) instrument = 2;
        if (below == Material.glass) instrument = 3;
        if (below == Material.wood) instrument = 4;
        world.addBlockEvent(x, y, z, Blocks.noteblock, instrument, note);
    }
}
