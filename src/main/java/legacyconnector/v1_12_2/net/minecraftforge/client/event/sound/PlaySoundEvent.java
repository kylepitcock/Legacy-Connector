package legacyconnector.v1_12_2.net.minecraftforge.client.event.sound;

import legacyconnector.v1_12_2.net.minecraft.client.audio.ISound;

/** A sound is about to play: handlers may replace it ({@link #setResultSound}) or silence it (null). */
public class PlaySoundEvent extends SoundEvent {
    private final String name;
    private final ISound sound;
    private ISound result;

    public PlaySoundEvent(legacyconnector.v1_12_2.net.minecraft.client.audio.SoundManager manager, ISound sound) {
        this(sound);
    }

    public PlaySoundEvent(ISound sound) {
        this.sound = sound;
        this.name = sound == null || sound.getSoundLocation() == null ? "" : sound.getSoundLocation().getPath();
        this.result = sound;
    }

    public String getName() {
        return name;
    }

    public ISound getSound() {
        return sound;
    }

    public ISound getResultSound() {
        return result;
    }

    public void setResultSound(ISound result) {
        this.result = result;
    }
}
