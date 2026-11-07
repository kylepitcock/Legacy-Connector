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
 * pool only remembers the names (for the lookups mods make: {@link #getRandomSoundFromSoundPool}, {@link #getRandomSound}).
 */
public class SoundPool {
    public static final SoundPool SOUNDS = new SoundPool();
    public static final SoundPool STREAMING = new SoundPool();
    public static final SoundPool MUSIC = new SoundPool();

    private final Map<String, List<String>> byEvent = new LinkedHashMap<>();
    private final List<String> all = new ArrayList<>();
    private final Random random = new Random();
    public boolean isGetRandomSound = true;
    public int numberOfSoundPoolEntries;

    /** {@code addSound("mod/boom1.ogg")}: the file name without the digits and extension is the event, as in 1.6.4. */
    public synchronized Object addSound(String name) {
        if (name == null) return null;
        String event = name;
        int dot = event.lastIndexOf('.');
        if (dot >= 0) event = event.substring(0, dot);
        event = event.replaceAll("[0-9]+$", "").replace('/', '.');
        byEvent.computeIfAbsent(event, k -> new ArrayList<>()).add(name);
        all.add(name);
        numberOfSoundPoolEntries++;
        return name;
    }

    public Object addSound(String name, File file) {
        return addSound(name);
    }

    /** A random sound of the event ("mod.boom"), as the name it was added under; null when nothing was added for it. */
    public synchronized Object getRandomSoundFromSoundPool(String event) {
        List<String> sounds = byEvent.get(event);
        return sounds == null || sounds.isEmpty() ? null : sounds.get(random.nextInt(sounds.size()));
    }

    public synchronized Object getRandomSound() {
        return all.isEmpty() ? null : all.get(random.nextInt(all.size()));
    }

    /** The names added so far (tests and diagnostics). */
    public synchronized List<String> names() {
        return new ArrayList<>(all);
    }
}
