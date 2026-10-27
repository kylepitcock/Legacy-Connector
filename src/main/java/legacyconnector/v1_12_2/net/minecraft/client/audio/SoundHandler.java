package legacyconnector.v1_12_2.net.minecraft.client.audio;

/**
 * The client's sound player. Legacy sounds are played as modern sound instances (tickable ones are updated every tick); each played sound
 * is remembered with its instance so {@link #isSoundPlaying} and {@link #stopSound} work on it.
 */
public class SoundHandler implements legacyconnector.v1_12_2.net.minecraft.client.resources.IResourceManagerReloadListener,
        legacyconnector.v1_12_2.net.minecraft.util.ITickable {
    public static final SoundHandler mcc$INSTANCE = new SoundHandler();

    public void playSound(ISound sound) {
        org.kylepitcock.legacyconnector.v1_12_2.client.LegacySounds1122.play(sound, 0);
    }

    public void playDelayedSound(ISound sound, int delay) {
        org.kylepitcock.legacyconnector.v1_12_2.client.LegacySounds1122.play(sound, delay);
    }

    public boolean isSoundPlaying(ISound sound) {
        return org.kylepitcock.legacyconnector.v1_12_2.client.LegacySounds1122.isPlaying(sound);
    }

    public void stopSound(ISound sound) {
        org.kylepitcock.legacyconnector.v1_12_2.client.LegacySounds1122.stop(sound);
    }

    public void stopSounds() {
        net.minecraft.client.Minecraft.getInstance().getSoundManager().stop();
    }

    public void pauseSounds() {
        net.minecraft.client.Minecraft.getInstance().getSoundManager().pause();
    }

    public void resumeSounds() {
        net.minecraft.client.Minecraft.getInstance().getSoundManager().resume();
    }

    public void setSoundLevel(legacyconnector.v1_12_2.net.minecraft.util.SoundCategory category, float volume) {
        net.minecraft.client.Minecraft.getInstance().options.getSoundSourceOptionInstance(net.minecraft.sounds.SoundSource.valueOf(category.name())).set((double) volume);
    }

    /** The modern sound manager ticks the sounds. */
    @Override
    public void update() {
    }

    @Override
    public void onResourceManagerReload(legacyconnector.v1_12_2.net.minecraft.client.resources.IResourceManager resourceManager) {
    }
}
