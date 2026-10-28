package legacyconnector.v1_12_2.net.minecraftforge.event.world;

import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Cancelable;

/** Forge 1.12.2 note block events (fed from the modern ones): a note block plays (Play) or is retuned (Change); both cancelable. */
public class NoteBlockEvent extends BlockEvent {
    private int noteId;

    protected NoteBlockEvent(World world, BlockPos pos, IBlockState state, int note) {
        super(world, pos, state);
        this.noteId = note;
    }

    public Octave getOctave() {
        return Octave.values()[Math.min(noteId / 12, Octave.values().length - 1)];
    }

    public Note getNote() {
        return Note.values()[noteId % 12];
    }

    public int getVanillaNoteId() {
        return noteId;
    }

    public void setNote(Note note, Octave octave) {
        if (octave == Octave.HIGH && note != Note.F_SHARP) throw new IllegalArgumentException("Octave.HIGH is only valid for Note.F_SHARP!");
        this.noteId = note.ordinal() + octave.ordinal() * 12;
    }

    @Cancelable
    public static class Play extends NoteBlockEvent {
        private Instrument instrument;

        public Play(World world, BlockPos pos, IBlockState state, int note, int instrument) {
            super(world, pos, state, note);
            this.instrument = Instrument.fromId(instrument);
        }

        public Instrument getInstrument() {
            return instrument;
        }

        public void setInstrument(Instrument instrument) {
            this.instrument = instrument;
        }
    }

    @Cancelable
    public static class Change extends NoteBlockEvent {
        private final Note oldNote;
        private final Octave oldOctave;

        public Change(World world, BlockPos pos, IBlockState state, int oldNote, int newNote) {
            super(world, pos, state, newNote);
            this.oldNote = Note.values()[oldNote % 12];
            this.oldOctave = Octave.values()[Math.min(oldNote / 12, Octave.values().length - 1)];
        }

        public Note getOldNote() {
            return oldNote;
        }

        public Octave getOldOctave() {
            return oldOctave;
        }
    }

    public enum Instrument {
        PIANO, BASSDRUM, SNARE, CLICKS, BASSGUITAR, FLUTE, BELL, GUITAR, CHIME, XYLOPHONE;

        static Instrument fromId(int id) {
            return id < 0 || id >= values().length ? PIANO : values()[id];
        }
    }

    public enum Note {
        F_SHARP, G, G_SHARP, A, A_SHARP, B, C, C_SHARP, D, D_SHARP, E, F
    }

    public enum Octave {
        LOW, MID, HIGH
    }
}
