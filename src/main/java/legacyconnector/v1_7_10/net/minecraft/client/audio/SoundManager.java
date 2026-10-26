package legacyconnector.v1_7_10.net.minecraft.client.audio;

/**
 * 1.7.10's sound system owner behind the {@link SoundHandler} (sound events carry it as {@code manager}; mods replay a
 * changed sound through it). Everything goes to the modern sound manager through the handler.
 */
public class SoundManager {
    public static final SoundManager INSTANCE = new SoundManager(SoundHandler.INSTANCE);
    private final SoundHandler sndHandler;
    /** The sound library mods drive directly (IC2 takes it by reflection): its sources play on the modern engine. */
    private final legacyconnector.v1_7_10.paulscode.sound.SoundSystem sndSystem = new legacyconnector.v1_7_10.paulscode.sound.SoundSystem();
    /** Whether the engine is up (mods wait for it by reflection before using {@link #sndSystem}); kept by {@link #mcc$engineLoaded}. */
    private boolean loaded;

    public SoundManager(SoundHandler handler) {
        this.sndHandler = handler;
        if (handler != null) handler.sndManager = this;
    }

    /** The modern engine (re)loaded: legacy sources rebuild their buffers and {@code loaded} follows the engine. */
    public void mcc$engineLoaded(boolean engineLoaded) {
        this.loaded = engineLoaded;
    }

    public void reloadSoundSystem() {
        net.minecraft.client.Minecraft.getInstance().getSoundManager().reload();
    }

    public void setSoundCategoryVolume(SoundCategory category, float volume) {
        if (category != null) net.minecraft.client.Minecraft.getInstance().getSoundManager().updateSourceVolume(category.mcc$source(), volume);
    }

    public void unloadSoundSystem() {
    }

    public void stopAllSounds() {
        sndHandler.stopSounds();
    }

    public void updateAllSounds() {
    }

    public boolean isSoundPlaying(ISound sound) {
        return sndHandler.isSoundPlaying(sound);
    }

    public void stopSound(ISound sound) {
        sndHandler.stopSound(sound);
    }

    public void playSound(ISound sound) {
        sndHandler.playSound(sound);
    }

    public void pauseAllSounds() {
        sndHandler.pauseSounds();
    }

    public void resumeAllSounds() {
        sndHandler.resumeSounds();
    }

    public void addDelayedSound(ISound sound, int delayTicks) {
        sndHandler.playDelayedSound(sound, delayTicks);
    }

    public void setListener(legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer player, float partialTicks) {
    }
}
