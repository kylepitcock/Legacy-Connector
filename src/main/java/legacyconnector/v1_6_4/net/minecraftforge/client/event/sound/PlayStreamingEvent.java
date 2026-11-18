package legacyconnector.v1_6_4.net.minecraftforge.client.event.sound;

import legacyconnector.v1_6_4.net.minecraft.client.audio.SoundPoolEntry;
import legacyconnector.v1_7_10.net.minecraft.client.audio.SoundManager;
import legacyconnector.v1_7_10.net.minecraftforge.client.event.sound.SoundEvent;

/**
 * Forge 1.5.2/1.6.4's {@code PlayStreamingEvent}: fired before the client plays a streamed sound (a music disc in a jukebox), with the stream's name and
 * position. {@link #result} is the entry that will play: a handler sets it to null to silence the stream (the layer silences it; it cannot swap the
 * stream for another).
 */
public class PlayStreamingEvent extends SoundEvent {
    public final String name;
    public final float x;
    public final float y;
    public final float z;
    public SoundPoolEntry result;

    public PlayStreamingEvent(SoundManager manager, String name, float x, float y, float z) {
        super(manager);
        this.name = name;
        this.x = x;
        this.y = y;
        this.z = z;
        this.result = new SoundPoolEntry(name, null);
    }
}
