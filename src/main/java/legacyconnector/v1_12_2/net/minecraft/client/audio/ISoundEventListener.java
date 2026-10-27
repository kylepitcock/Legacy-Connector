package legacyconnector.v1_12_2.net.minecraft.client.audio;

/** Told about every sound that starts playing (the subtitles overlay is one). */
public interface ISoundEventListener {
    void soundPlay(ISound sound, SoundEventAccessor accessor);
}
