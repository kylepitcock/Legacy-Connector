package legacyconnector.v1_6_4.net.minecraftforge.client.event.sound;

import legacyconnector.v1_6_4.net.minecraft.client.audio.SoundPoolEntry;
import legacyconnector.v1_7_10.net.minecraft.client.audio.SoundManager;
import legacyconnector.v1_7_10.net.minecraftforge.client.event.sound.SoundEvent;

/**
 * Forge 1.5.2/1.6.4's {@code PlaySoundEvent}: fired before the client plays a sound effect, with the sound's name, position, volume and pitch.
 * {@link #result} is the sound pool entry that will play: a handler sets it to null to silence the sound (the layer silences it; it cannot swap
 * the sound for another).
 */
public class PlaySoundEvent extends SoundEvent {
    public final String name;
    public final float x;
    public final float y;
    public final float z;
    public final float volume;
    public final float pitch;
    public SoundPoolEntry result;

    public PlaySoundEvent(SoundManager manager, String name, float x, float y, float z, float volume, float pitch) {
        super(manager);
        this.name = name;
        this.x = x;
        this.y = y;
        this.z = z;
        this.volume = volume;
        this.pitch = pitch;
        this.result = new SoundPoolEntry(name, null);
    }
}
