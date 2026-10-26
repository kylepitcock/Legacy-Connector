package legacyconnector.v1_7_10.net.minecraft.client.audio;

import java.util.ArrayList;
import java.util.List;

/** One event of a sounds.json: its sound files or events, whether it replaces earlier packs' entries, and its category. */
public class SoundList {
    private final List field_148577_a = new ArrayList();
    private boolean replaceExisting;
    private SoundCategory field_148576_c;

    public List getSoundList() {
        return field_148577_a;
    }

    public boolean canReplaceExisting() {
        return replaceExisting;
    }

    public void setReplaceExisting(boolean replace) {
        this.replaceExisting = replace;
    }

    public SoundCategory getSoundCategory() {
        return field_148576_c;
    }

    public void setSoundCategory(SoundCategory category) {
        this.field_148576_c = category;
    }

    /** One sound of an event: a file (or another event), with its volume, pitch, weight and streaming flag. */
    public static class SoundEntry {
        private String field_148569_a;
        private float field_148567_b = 1.0F;
        private float field_148568_c = 1.0F;
        private int field_148565_d = 1;
        private Type field_148566_e = Type.FILE;
        private boolean field_148564_f;

        public String getSoundEntryName() {
            return field_148569_a;
        }

        public void setSoundEntryName(String name) {
            this.field_148569_a = name;
        }

        public float getSoundEntryVolume() {
            return field_148567_b;
        }

        public void setSoundEntryVolume(float volume) {
            this.field_148567_b = volume;
        }

        public float getSoundEntryPitch() {
            return field_148568_c;
        }

        public void setSoundEntryPitch(float pitch) {
            this.field_148568_c = pitch;
        }

        public int getSoundEntryWeight() {
            return field_148565_d;
        }

        public void setSoundEntryWeight(int weight) {
            this.field_148565_d = weight;
        }

        public Type getSoundEntryType() {
            return field_148566_e;
        }

        public void setSoundEntryType(Type type) {
            this.field_148566_e = type;
        }

        public boolean isStreaming() {
            return field_148564_f;
        }

        public void setStreaming(boolean streaming) {
            this.field_148564_f = streaming;
        }

        public enum Type {
            FILE("file"),
            SOUND_EVENT("event");

            private final String field_148583_c;

            Type(String name) {
                this.field_148583_c = name;
            }

            public static Type getType(String name) {
                for (Type t : values()) if (t.field_148583_c.equals(name)) return t;
                return null;
            }
        }
    }
}
