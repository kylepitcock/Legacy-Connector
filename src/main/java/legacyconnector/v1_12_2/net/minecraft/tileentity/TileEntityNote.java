package legacyconnector.v1_12_2.net.minecraft.tileentity;

import legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/** 1.12.2's note block tile entity: the note (0-24) and playing it with the instrument of the block below (mods' note blocks extend it). */
public class TileEntityNote extends TileEntity {
    public byte note;
    public boolean previousRedstoneState;

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        compound.setByte("note", note);
        compound.setBoolean("powered", previousRedstoneState);
        return compound;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        note = compound.getByte("note");
        note = (byte) Math.max(0, Math.min(24, note));
        previousRedstoneState = compound.getBoolean("powered");
    }

    /** One note higher (24 wraps to 0). */
    public void changePitch() {
        note = (byte) ((note + 1) % 25);
        markDirty();
    }

    /** Plays the note when the block above is air: the instrument of the block below, the pitch of the note, and a note particle. */
    public void triggerNote(World worldIn, BlockPos posIn) {
        var level = worldIn == null ? null : worldIn.mcc$level();
        if (level == null) return;
        var p = new net.minecraft.core.BlockPos(posIn.getX(), posIn.getY(), posIn.getZ());
        if (!level.getBlockState(p.above()).isAir()) return;
        var instrument = level.getBlockState(p.below()).instrument();
        float pitch = net.minecraft.world.level.block.NoteBlock.getPitchFromNote(note);
        level.playSound(null, p, instrument.getSoundEvent().value(), net.minecraft.sounds.SoundSource.RECORDS, 3.0F, pitch);
        if (level instanceof net.minecraft.server.level.ServerLevel server) {
            server.sendParticles(net.minecraft.core.particles.ParticleTypes.NOTE, p.getX() + 0.5D, p.getY() + 1.2D, p.getZ() + 0.5D, 0, (double) note / 24.0D, 0.0D, 0.0D, 1.0D);
        }
    }
}
