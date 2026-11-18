package legacyconnector.v1_7_10.net.minecraft.client.audio;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import legacyconnector.v1_7_10.net.minecraft.util.ResourceLocation;

/**
 * A sound event of sounds.json: the pool of sounds it picks from by weight, its category and the pitch and volume the pool plays at.
 * {@link SoundHandler#getSound} returns one for every sound the game knows; the ones mods build and register
 * ({@link SoundRegistry#registerSound}) are looked up before them.
 */
public class SoundEventAccessorComposite implements ISoundEventAccessor {
    private final List soundPool = new ArrayList();
    private final Random rnd = new Random();
    private final ResourceLocation soundLocation;
    private final SoundCategory category;
    private final double eventPitch;
    private final double eventVolume;

    public SoundEventAccessorComposite(ResourceLocation location, double pitch, double volume, SoundCategory category) {
        this.soundLocation = location;
        this.eventPitch = pitch;
        this.eventVolume = volume;
        this.category = category;
    }

    public int getWeight() {
        int total = 0;
        for (Object o : soundPool) total += ((ISoundEventAccessor) o).getWeight();
        return total;
    }

    /** A sound of the pool chosen by weight; {@link SoundHandler#missing_sound} for an empty pool. */
    public SoundPoolEntry func_148720_g() {
        int total = getWeight();
        if (soundPool.isEmpty() || total == 0) return SoundHandler.missing_sound;
        int pick = rnd.nextInt(total);
        for (Object o : soundPool) {
            ISoundEventAccessor accessor = (ISoundEventAccessor) o;
            pick -= accessor.getWeight();
            if (pick < 0) {
                SoundPoolEntry entry = new SoundPoolEntry((SoundPoolEntry) accessor.func_148720_g());
                entry.setPitch(entry.getPitch() * eventPitch);
                entry.setVolume(entry.getVolume() * eventVolume);
                return entry;
            }
        }
        return SoundHandler.missing_sound;
    }

    public void addSoundToEventPool(ISoundEventAccessor accessor) {
        soundPool.add(accessor);
    }

    public ResourceLocation getSoundEventLocation() {
        return soundLocation;
    }

    public SoundCategory getSoundCategory() {
        return category;
    }
}
