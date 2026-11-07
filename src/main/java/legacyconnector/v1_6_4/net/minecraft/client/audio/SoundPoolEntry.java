package legacyconnector.v1_6_4.net.minecraft.client.audio;

import java.net.URL;

/** 1.6.4's {@code SoundPoolEntry}: one sound file of a pool (its name and where it can be read from), with the pitch and volume it plays at. */
public class SoundPoolEntry {
    private final String soundName;
    private final URL soundUrl;

    public SoundPoolEntry(String name, URL url) {
        this.soundName = name;
        this.soundUrl = url;
    }

    public String getSoundName() {
        return soundName;
    }

    public URL getSoundUrl() {
        return soundUrl;
    }

    public double getPitch() {
        return 1.0D;
    }

    public double getVolume() {
        return 1.0D;
    }

    @Override
    public String toString() {
        return "SoundPoolEntry{" + soundName + "}";
    }
}
