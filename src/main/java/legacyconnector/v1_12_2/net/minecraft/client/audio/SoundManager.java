package legacyconnector.v1_12_2.net.minecraft.client.audio;

/**
 * The sound engine behind the sound handler. Sounds play through the game's engine; 1.12's paulscode sound system ({@code sndSystem}) is
 * emulated for mods that drive raw sources by name: a source made from a URL streams through the game's engine.
 */
public class SoundManager {
    public static final SoundManager mcc$INSTANCE = new SoundManager(SoundHandler.mcc$INSTANCE, null);

    public final SoundHandler sndHandler;
    /** The paulscode sound system as mods reach it by reflection (named sources). */
    public final SoundSystemStarterThread sndSystem = new SoundSystemStarterThread();

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

    /**
     * 1.12's paulscode sound system: sources by name, made from a URL (Ogg Vorbis data plays; other codecs stay silent). Calls are
     * ignored off the client.
     */
    public static class SoundSystemStarterThread {
        private static boolean client() {
            return net.neoforged.fml.loading.FMLEnvironment.dist.isClient();
        }

        public void newSource(boolean priority, String sourcename, java.net.URL url, String identifier, boolean toLoop, float x, float y, float z,
                int attModel, float distOrRoll) {
            if (client()) org.kylepitcock.legacyconnector.v1_12_2.client.LegacySoundSystem1122.newSource(priority, sourcename, url, identifier, toLoop, x, y, z, attModel, distOrRoll);
        }

        /** A streamed source (music); played as {@link #newSource} does, which streams too. */
        public void newStreamingSource(boolean priority, String sourcename, java.net.URL url, String identifier, boolean toLoop, float x, float y, float z,
                int attModel, float distOrRoll) {
            newSource(priority, sourcename, url, identifier, toLoop, x, y, z, attModel, distOrRoll);
        }

        /** Re-enables a culled source; sources are never culled here. */
        public void activate(String sourcename) {
        }

        public void play(String sourcename) {
            if (client()) org.kylepitcock.legacyconnector.v1_12_2.client.LegacySoundSystem1122.play(sourcename);
        }

        public boolean playing(String sourcename) {
            return client() && org.kylepitcock.legacyconnector.v1_12_2.client.LegacySoundSystem1122.playing(sourcename);
        }

        public void stop(String sourcename) {
            if (client()) org.kylepitcock.legacyconnector.v1_12_2.client.LegacySoundSystem1122.stop(sourcename);
        }

        public void removeSource(String sourcename) {
            if (client()) org.kylepitcock.legacyconnector.v1_12_2.client.LegacySoundSystem1122.removeSource(sourcename);
        }

        public void setPosition(String sourcename, float x, float y, float z) {
            if (client()) org.kylepitcock.legacyconnector.v1_12_2.client.LegacySoundSystem1122.setPosition(sourcename, x, y, z);
        }

        public void setVolume(String sourcename, float value) {
            if (client()) org.kylepitcock.legacyconnector.v1_12_2.client.LegacySoundSystem1122.setVolume(sourcename, value);
        }

        public void setPitch(String sourcename, float value) {
            if (client()) org.kylepitcock.legacyconnector.v1_12_2.client.LegacySoundSystem1122.setPitch(sourcename, value);
        }
    }
}
