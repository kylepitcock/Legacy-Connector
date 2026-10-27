package legacyconnector.v1_12_2.net.minecraft.client.audio;

/**
 * The sound engine behind the sound handler. Sounds play through the game's engine; 1.12's paulscode sound system ({@code sndSystem}) is
 * gone, so mods that drive raw sources by name get nothing.
 */
public class SoundManager {
    public static final SoundManager mcc$INSTANCE = new SoundManager(SoundHandler.mcc$INSTANCE, null);

    public final SoundHandler sndHandler;

    public SoundManager(SoundHandler handler, legacyconnector.v1_12_2.net.minecraft.client.settings.GameSettings options) {
        this.sndHandler = handler;
    }

    public void playSound(ISound sound) {
        sndHandler.playSound(sound);
    }

    public void playDelayedSound(ISound sound, int delay) {
        sndHandler.playDelayedSound(sound, delay);
    }

    public boolean isSoundPlaying(ISound sound) {
        return sndHandler.isSoundPlaying(sound);
    }

    public void stopSound(ISound sound) {
        sndHandler.stopSound(sound);
    }

    public void stopAllSounds() {
        sndHandler.stopSounds();
    }

    public void pauseAllSounds() {
        sndHandler.pauseSounds();
    }

    public void resumeAllSounds() {
        sndHandler.resumeSounds();
    }

    public void setVolume(legacyconnector.v1_12_2.net.minecraft.util.SoundCategory category, float volume) {
        sndHandler.setSoundLevel(category, volume);
    }
}
