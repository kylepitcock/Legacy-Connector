package legacyconnector.v1_12_2.net.minecraftforge.client.event.sound;

/** A sound started playing on a source of the sound engine (posted from the game's event). */
public class PlayStreamingSourceEvent extends SoundEvent.SoundSourceEvent {
    public PlayStreamingSourceEvent(legacyconnector.v1_12_2.net.minecraft.client.audio.SoundManager manager, legacyconnector.v1_12_2.net.minecraft.client.audio.ISound sound,
            String uuid) {
        super(manager, sound, uuid);
    }
}
