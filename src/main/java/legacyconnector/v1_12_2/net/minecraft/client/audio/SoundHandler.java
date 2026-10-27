package legacyconnector.v1_12_2.net.minecraft.client.audio;

/**
 * The client's sound player. Legacy sounds are played as modern sound instances (tickable ones are updated every tick); each played sound
 * is remembered with its instance so {@link #isSoundPlaying} and {@link #stopSound} work on it.
 */
public class SoundHandler implements legacyconnector.v1_12_2.net.minecraft.client.resources.IResourceManagerReloadListener,
        legacyconnector.v1_12_2.net.minecraft.util.ITickable {
    public static final SoundHandler mcc$INSTANCE = new SoundHandler();
    public static final Sound MISSING_SOUND = new Sound("meta:missing_sound", 1.0F, 1.0F, 1, Sound.Type.FILE, false);
    private final java.util.Map<ISoundEventListener, net.minecraft.client.sounds.SoundEventListener> listeners = new java.util.concurrent.ConcurrentHashMap<>();

    /** Phantom: the sound engine (redirected to {@link #mcc$get$sndManager}). */
    @Deprecated
    public SoundManager sndManager;

    public static SoundManager mcc$get$sndManager(SoundHandler h) {
        return SoundManager.mcc$INSTANCE;
    }

    /** The sound event of sounds.json with this name, or null when there is none. */
    public SoundEventAccessor getAccessor(legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation location) {
        if (location == null) return null;
        return SoundEventAccessor.mcc$of(location, net.minecraft.client.Minecraft.getInstance().getSoundManager().getSoundEvent(location.mcc$modern()));
    }

    /** Told about every sound that starts (with the legacy view of the sound and its event). */
    public void addListener(ISoundEventListener listener) {
        if (listener == null) return;
        net.minecraft.client.sounds.SoundEventListener modern = (sound, accessor, range) -> {
            var id = sound.getLocation();
            var location = new legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation(id.getNamespace(), id.getPath());
            listener.soundPlay(org.kylepitcock.legacyconnector.v1_12_2.client.LegacySounds1122.legacy(sound), SoundEventAccessor.mcc$of(location, accessor));
        };
        if (listeners.putIfAbsent(listener, modern) == null) net.minecraft.client.Minecraft.getInstance().getSoundManager().addListener(modern);
    }

    public void removeListener(ISoundEventListener listener) {
        var modern = listener == null ? null : listeners.remove(listener);
        if (modern != null) net.minecraft.client.Minecraft.getInstance().getSoundManager().removeListener(modern);
    }

    /** Stops the sounds with this event name (all when empty) in the category (all when null). */
    public void stop(String sound, legacyconnector.v1_12_2.net.minecraft.util.SoundCategory category) {
        var id = sound == null || sound.isEmpty() ? null : net.minecraft.resources.ResourceLocation.tryParse(sound);
        net.minecraft.sounds.SoundSource source = null;
        if (category != null) {
            try {
                source = net.minecraft.sounds.SoundSource.valueOf(category.name());
            } catch (IllegalArgumentException e) {
                source = null;
            }
        }
        net.minecraft.client.Minecraft.getInstance().getSoundManager().stop(id, source);
    }

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
