package legacyconnector.v1_6_4.net.minecraftforge.client.event.sound;

import legacyconnector.v1_6_4.net.minecraft.client.audio.SoundPoolEntry;
import legacyconnector.v1_7_10.net.minecraft.client.audio.SoundManager;
import legacyconnector.v1_7_10.net.minecraftforge.client.event.sound.SoundEvent;

/**
 * Forge 1.5.2/1.6.4's {@code PlayBackgroundMusicEvent}: fired before the client starts a piece of background music. {@link #result} is the entry that will
 * play: a handler sets it to null to silence the track (the layer silences it; it cannot swap the track for another).
 */
public class PlayBackgroundMusicEvent extends SoundEvent {
    public SoundPoolEntry result;

    public PlayBackgroundMusicEvent(SoundManager manager, SoundPoolEntry result) {
        super(manager);
        this.result = result;
    }
}
