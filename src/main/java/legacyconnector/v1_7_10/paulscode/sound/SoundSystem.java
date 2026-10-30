package legacyconnector.v1_7_10.paulscode.sound;

import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.kylepitcock.legacyconnector.v1_7_10.client.LegacySoundSources;

/**
 * 1.7.10's sound library as mods drive it directly (IC2 takes the game's instance from its {@code SoundManager} and
 * plays machine loops on named sources). Sources are played on the modern sound engine by
 * {@link LegacySoundSources}; positions are world coordinates heard from the player, as before.
 */
public class SoundSystem {
    private final Map<String, LegacySoundSources.Source> sources = new ConcurrentHashMap<>();
    /** paulscode's library; mods reflect on this field to reach its stream thread (DragonAPI). */
    private final Library soundLibrary = new Library();
    private volatile float masterVolume = 1.0F;
    private int quickPlayCount;

    public SoundSystem() {
    }

    private static URL mcc$resource(String filename) {
        if (filename == null) return null;
        URL url = SoundSystem.class.getClassLoader().getResource(filename);
        if (url == null) url = Thread.currentThread().getContextClassLoader().getResource(filename);
        return url;
    }

    public void newSource(boolean priority, String sourcename, URL url, String identifier, boolean toLoop, float x, float y, float z,
            int attmodel, float distOrRoll) {
        if (sourcename == null || url == null) return;
        LegacySoundSources.Source old = sources.put(sourcename, new LegacySoundSources.Source(url, identifier, toLoop, x, y, z, attmodel, distOrRoll, false));
        if (old != null) old.stop();
    }

    public void newSource(boolean priority, String sourcename, String filename, boolean toLoop, float x, float y, float z, int attmodel, float distOrRoll) {
        newSource(priority, sourcename, mcc$resource(filename), filename, toLoop, x, y, z, attmodel, distOrRoll);
    }

    /** A streaming source: played like a normal one (the files mods stream are short enough to decode whole). */
    public void newStreamingSource(boolean priority, String sourcename, URL url, String identifier, boolean toLoop, float x, float y, float z,
            int attmodel, float distOrRoll) {
        newSource(priority, sourcename, url, identifier, toLoop, x, y, z, attmodel, distOrRoll);
    }

    public void newStreamingSource(boolean priority, String sourcename, String filename, boolean toLoop, float x, float y, float z,
            int attmodel, float distOrRoll) {
        newSource(priority, sourcename, filename, toLoop, x, y, z, attmodel, distOrRoll);
    }

    /** Plays a file once (or looping) on a temporary source; the source's name, so it can be changed while it plays. */
    public String quickPlay(boolean priority, URL url, String identifier, boolean toLoop, float x, float y, float z, int attmodel, float distOrRoll) {
        if (url == null) return null;
        removeTemporarySources();
        String name;
        synchronized (this) {
            name = "Source_" + quickPlayCount++;
        }
        LegacySoundSources.Source source = new LegacySoundSources.Source(url, identifier, toLoop, x, y, z, attmodel, distOrRoll, true);
        sources.put(name, source);
        source.play(masterVolume);
        return name;
    }

    public String quickPlay(boolean priority, String filename, boolean toLoop, float x, float y, float z, int attmodel, float distOrRoll) {
        return quickPlay(priority, mcc$resource(filename), filename, toLoop, x, y, z, attmodel, distOrRoll);
    }

    public String quickStream(boolean priority, URL url, String identifier, boolean toLoop, float x, float y, float z, int attmodel, float distOrRoll) {
        return quickPlay(priority, url, identifier, toLoop, x, y, z, attmodel, distOrRoll);
    }

    /** Drops the temporary sources that finished playing. */
    public void removeTemporarySources() {
        sources.entrySet().removeIf(e -> e.getValue().temporaryDone());
    }

    public void play(String sourcename) {
        LegacySoundSources.Source s = sources.get(sourcename);
        if (s != null) s.play(masterVolume);
    }

    public void pause(String sourcename) {
        LegacySoundSources.Source s = sources.get(sourcename);
        if (s != null) s.pause();
    }

    public void stop(String sourcename) {
        LegacySoundSources.Source s = sources.get(sourcename);
        if (s != null) s.stop();
    }

    public void rewind(String sourcename) {
        LegacySoundSources.Source s = sources.get(sourcename);
        if (s == null) return;
        boolean playing = s.playing();
        s.stop();
        if (playing) s.play(masterVolume);
    }

    /** Streaming sources drop their queued data; the sources here play from memory, so there is nothing to drop. */
    public void flush(String sourcename) {
    }

    /** Stops the source and frees its channel (out of hearing range); {@link #activate} makes it playable again. */
    public void cull(String sourcename) {
        LegacySoundSources.Source s = sources.get(sourcename);
        if (s != null) s.cull();
    }

    public void activate(String sourcename) {
        LegacySoundSources.Source s = sources.get(sourcename);
        if (s != null) s.activate(masterVolume);
    }

    public void removeSource(String sourcename) {
        LegacySoundSources.Source s = sources.remove(sourcename);
        if (s != null) s.stop();
    }

    public boolean playing(String sourcename) {
        LegacySoundSources.Source s = sources.get(sourcename);
        return s != null && s.playing();
    }

    public boolean playing() {
        for (LegacySoundSources.Source s : sources.values()) if (s.playing()) return true;
        return false;
    }

    public void setPosition(String sourcename, float x, float y, float z) {
        LegacySoundSources.Source s = sources.get(sourcename);
        if (s == null) return;
        s.x = x;
        s.y = y;
        s.z = z;
        s.update(masterVolume);
    }

    public void setVolume(String sourcename, float volume) {
        LegacySoundSources.Source s = sources.get(sourcename);
        if (s == null) return;
        s.volume = Math.max(0.0F, Math.min(1.0F, volume));
        s.update(masterVolume);
    }

    public float getVolume(String sourcename) {
        LegacySoundSources.Source s = sources.get(sourcename);
        return s == null ? 0.0F : s.volume;
    }

    public void setPitch(String sourcename, float pitch) {
        LegacySoundSources.Source s = sources.get(sourcename);
        if (s == null) return;
        s.pitch = Math.max(0.5F, Math.min(2.0F, pitch));
        s.update(masterVolume);
    }

    public float getPitch(String sourcename) {
        LegacySoundSources.Source s = sources.get(sourcename);
        return s == null ? 1.0F : s.pitch;
    }

    public void setLooping(String sourcename, boolean loop) {
        LegacySoundSources.Source s = sources.get(sourcename);
        if (s == null) return;
        s.loop = loop;
        s.update(masterVolume);
    }

    public void setAttenuation(String sourcename, int model) {
        LegacySoundSources.Source s = sources.get(sourcename);
        if (s == null) return;
        s.attenuation = model;
        s.update(masterVolume);
    }

    /** The fade distance (linear attenuation) or the roll-off factor (roll-off attenuation). */
    public void setDistOrRoll(String sourcename, float distOrRoll) {
        LegacySoundSources.Source s = sources.get(sourcename);
        if (s == null) return;
        s.distOrRoll = distOrRoll;
        s.update(masterVolume);
    }

    public void setPriority(String sourcename, boolean priority) {
    }

    public void setVelocity(String sourcename, float x, float y, float z) {
    }

    public void setMasterVolume(float volume) {
        masterVolume = Math.max(0.0F, Math.min(1.0F, volume));
        for (LegacySoundSources.Source s : sources.values()) s.update(masterVolume);
    }

    public float getMasterVolume() {
        return masterVolume;
    }

    /** The game's sound system keeps the listener on the camera itself. */
    public void setListenerPosition(float x, float y, float z) {
    }

    public void setListenerOrientation(float lookX, float lookY, float lookZ, float upX, float upY, float upZ) {
    }

    public void cleanup() {
        for (LegacySoundSources.Source s : sources.values()) s.stop();
        sources.clear();
    }
}
