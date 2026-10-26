package legacyconnector.v1_7_10.net.minecraftforge.event.world;

import legacyconnector.v1_7_10.net.minecraft.world.World;

/** A note block plays or is retuned. */
public class NoteBlockEvent extends BlockEvent {
    private int noteId;

    protected NoteBlockEvent(World world, int x, int y, int z, int meta, int note) {
        super(x, y, z, world, legacyconnector.v1_7_10.net.minecraft.init.Blocks.noteblock, meta);
        this.noteId = note;
    }

    public Note getNote() {
        return Note.fromId(noteId);
    }

    public Octave getOctave() {
        return Octave.values()[Math.min(noteId / 12, 2)];
    }

    /** The note as vanilla numbers it, 0-24. */
    public int getVanillaNoteId() {
        return noteId;
    }

    protected void setNote(Note note, Octave octave) {
        if (octave == Octave.HIGH && note != Note.F_SHARP) throw new IllegalArgumentException("Octave.HIGH is only valid for Note.F_SHARP!");
        this.noteId = note.ordinal() + octave.ordinal() * 12;
    }

    /** A note block is played; cancel it to keep it silent. */
    @legacyconnector.v1_7_10.cpw.mods.fml.common.eventhandler.Cancelable
    public static class Play extends NoteBlockEvent {
        public Instrument instrument;

        public Play(World world, int x, int y, int z, int meta, int note, int instrument) {
            super(world, x, y, z, meta, note);
            this.instrument = Instrument.fromId(instrument);
        }

        @Override
        public void setNote(Note note, Octave octave) {
            super.setNote(note, octave);
        }
    }

    /** A note block is retuned by a player; cancel it to keep the old note. */
    @legacyconnector.v1_7_10.cpw.mods.fml.common.eventhandler.Cancelable
    public static class Change extends NoteBlockEvent {
        public final Note oldNote;
        public final Octave oldOctave;

        public Change(World world, int x, int y, int z, int meta, int oldNote, int newNote) {
            super(world, x, y, z, meta, newNote);
            this.oldNote = Note.fromId(oldNote);
            this.oldOctave = Octave.values()[Math.min(oldNote / 12, 2)];
        }

        @Override
        public void setNote(Note note, Octave octave) {
            super.setNote(note, octave);
        }
    }

    public enum Instrument {
        PIANO, BASSDRUM, SNARE, CLICKS, BASSGUITAR;

        static Instrument fromId(int id) {
            return id < 0 || id > 4 ? PIANO : values()[id];
        }
    }

    public enum Note {
        F_SHARP, G, G_SHARP, A, A_SHARP, B, C, C_SHARP, D, D_SHARP, E, F;

        static Note fromId(int id) {
            return values()[Math.floorMod(id, 12)];
        }
    }

    public enum Octave {
        LOW, MID, HIGH
    }
}
