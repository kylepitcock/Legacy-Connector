package legacyconnector.v1_6_4.net.minecraft.client.audio;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * 1.6.4's {@code SoundPool}: the three pools of the sound manager (sounds, streaming, music), into which mods put their {@code .ogg} files by name
 * ({@code event.manager.soundPoolSounds.addSound("mod/boom.ogg", file)}). Sounds play from the mod's {@code sounds.json} and resource packs here, so the
 * pool only remembers the entries (for the lookups mods make: {@link #getRandomSoundFromSoundPool}, {@link #getRandomSound}).
 */
public class SoundPool {
    public static final SoundPool SOUNDS = new SoundPool();
    public static final SoundPool STREAMING = new SoundPool();
    public static final SoundPool MUSIC = new SoundPool();

    private final Map<String, List<SoundPoolEntry>> byEvent = new LinkedHashMap<>();
    private final List<SoundPoolEntry> all = new ArrayList<>();
    private final Random random = new Random();
    public boolean isGetRandomSound = true;
    public int numberOfSoundPoolEntries;

    public SoundPool() {}

    /** {@code new SoundPool(resourceManager, "sound", true)}: the arguments only say where the files were read from. */
    public SoundPool(legacyconnector.v1_7_10.net.minecraft.client.resources.IResourceManager manager, String path, boolean random) {
        this.isGetRandomSound = random;
    }

    /** {@code addSound("mod/boom1.ogg")}: the file name without the digits and extension is the event, as in 1.6.4. */
    public void addSound(String name) {
        addSound(name, (File) null);
    }

    public synchronized SoundPoolEntry addSound(String name, File file) {
        if (name == null) return null;
        String event = name;
        int dot = event.lastIndexOf('.');
        if (dot >= 0) event = event.substring(0, dot);
        event = event.replaceAll("[0-9]+$", "").replace('/', '.');
        java.net.URL url = null;
        try {
            if (file != null) url = file.toURI().toURL();
        } catch (java.net.MalformedURLException e) {
            // an entry without a location: the sound still plays by name
        }
        SoundPoolEntry entry = new SoundPoolEntry(name, url);
        byEvent.computeIfAbsent(event, k -> new ArrayList<>()).add(entry);
        all.add(entry);
        numberOfSoundPoolEntries++;
        return entry;
    }

    /** A random sound of the event ("mod.boom"); null when nothing was added for it. */
    public synchronized SoundPoolEntry getRandomSoundFromSoundPool(String event) {
        List<SoundPoolEntry> sounds = byEvent.get(event);
        return sounds == null || sounds.isEmpty() ? null : sounds.get(random.nextInt(sounds.size()));
    }

    public synchronized SoundPoolEntry getRandomSound() {
        return all.isEmpty() ? null : all.get(random.nextInt(all.size()));
    }

    /** The names added so far (tests and diagnostics). */
    public synchronized List<String> names() {
        List<String> out = new ArrayList<>();
        for (SoundPoolEntry e : all) out.add(e.getSoundName());
        return out;
    }
}
