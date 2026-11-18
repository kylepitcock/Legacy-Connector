package legacyconnector.v1_7_10.net.minecraft.client.audio;

/**
 * 1.7.10's sound system owner behind the {@link SoundHandler} (sound events carry it as {@code manager}; mods replay a
 * changed sound through it). Everything goes to the modern sound manager through the handler.
 */
public class SoundManager {
    public static final SoundManager INSTANCE = new SoundManager(SoundHandler.INSTANCE);
    private final SoundHandler sndHandler;
    /** The sound library mods drive directly (IC2 takes it by reflection): its sources play on the modern engine. */
    private final SoundSystemStarterThread sndSystem = new SoundSystemStarterThread();

    /** 1.7.10's type of {@link #sndSystem} (a SoundSystem that queues commands on its own thread); mods name it when they read the field. */
    public static class SoundSystemStarterThread extends legacyconnector.v1_7_10.paulscode.sound.SoundSystem {
    }
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

    /** The volume slider of a category (the master category is always 1: the master volume scales the listener). */
    public float getSoundCategoryVolume(SoundCategory category) {
        if (category == null || category.mcc$source() == net.minecraft.sounds.SoundSource.MASTER) return 1.0F;
        return net.minecraft.client.Minecraft.getInstance().options.getSoundSourceVolume(category.mcc$source());
    }

    /** A URL that reads a resource of the packs ({@code mcsounddomain:} in 1.7.10), for mods that stream sounds themselves. */
    public static java.net.URL getURLForSoundResource(legacyconnector.v1_7_10.net.minecraft.util.ResourceLocation location) {
        if (location == null) return null;
        var id = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(location.getResourceDomain(), location.getResourcePath());
        try {
            return java.net.URL.of(java.net.URI.create("mcsounddomain:" + id), new java.net.URLStreamHandler() {
                @Override
                protected java.net.URLConnection openConnection(java.net.URL u) {
                    return new java.net.URLConnection(u) {
                        @Override
                        public void connect() {
                        }

                        @Override
                        public java.io.InputStream getInputStream() throws java.io.IOException {
                            return net.minecraft.client.Minecraft.getInstance().getResourceManager().open(id);
                        }
                    };
                }
            });
        } catch (java.net.MalformedURLException e) {
            return null;
        }
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

    // Members125 batch 2

    /** 1.2.5's {@code getSoundsPool()}: the pool mods add their sound files to (the same one the 1.6.4 field {@code soundPoolSounds} reads). */
    public legacyconnector.v1_6_4.net.minecraft.client.audio.SoundPool getSoundsPool() {
        return legacyconnector.v1_6_4.net.minecraft.client.audio.SoundPool.SOUNDS;
    }
}
