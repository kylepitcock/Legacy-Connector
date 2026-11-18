package legacyconnector.v1_7_10.net.minecraft.client.audio;

import legacyconnector.v1_7_10.net.minecraft.util.ResourceLocation;

/** One concrete sound file with the pitch and volume it plays at (what an event's pool hands out). */
public class SoundPoolEntry {
    private final ResourceLocation location;
    private double pitch;
    private double volume;
    private final boolean streaming;

    public SoundPoolEntry(ResourceLocation location, double pitch, double volume, boolean streaming) {
        this.location = location;
        this.pitch = pitch;
        this.volume = volume;
        this.streaming = streaming;
    }

    public SoundPoolEntry(SoundPoolEntry other) {
        this.location = other.location;
        this.pitch = other.pitch;
        this.volume = other.volume;
        this.streaming = other.streaming;
    }

    public ResourceLocation getSoundPoolEntryLocation() {
        return location;
    }

    public double getPitch() {
        return pitch;
    }

    public void setPitch(double pitch) {
        this.pitch = pitch;
    }

    public double getVolume() {
        return volume;
    }

    public void setVolume(double volume) {
        this.volume = volume;
    }

    public boolean isStreamingSound() {
        return streaming;
    }
}
