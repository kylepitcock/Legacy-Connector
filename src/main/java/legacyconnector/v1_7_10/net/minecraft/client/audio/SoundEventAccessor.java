package legacyconnector.v1_7_10.net.minecraft.client.audio;

/** A sound file in an event's pool, with its weight. */
public class SoundEventAccessor implements ISoundEventAccessor {
    private final SoundPoolEntry entry;
    private final int weight;

    public SoundEventAccessor(SoundPoolEntry entry, int weight) {
        this.entry = entry;
        this.weight = weight;
    }

    @Override
    public int getWeight() {
        return weight;
    }

    @Override
    public SoundPoolEntry func_148720_g() {
        return new SoundPoolEntry(entry);
    }
}
