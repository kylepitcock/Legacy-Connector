package legacyconnector.v1_12_2.net.minecraftforge.client.event.sound;

/** The sound engine (re)started (posted from the game's engine load event). */
public class SoundLoadEvent extends SoundEvent {
    public SoundLoadEvent(legacyconnector.v1_12_2.net.minecraft.client.audio.SoundManager manager) {
        super(manager);
    }
}
