package legacyconnector.v1_7_10.paulscode.sound;

import javax.sound.sampled.AudioFormat;

/** paulscode's block of decoded audio: the bytes and the format they are in (what codecs hand to the library). */
public class SoundBuffer {
    /** The sample data. */
    public byte[] audioData;
    /** The format of the sample data. */
    public AudioFormat audioFormat;

    public SoundBuffer(byte[] audioData, AudioFormat audioFormat) {
        this.audioData = audioData;
        this.audioFormat = audioFormat;
    }

    /** Frees the data. */
    public void cleanup() {
        audioData = null;
        audioFormat = null;
    }

    /** Keeps only the first {@code maxLength} bytes of the data (when it is longer). */
    public void trimData(int maxLength) {
        if (audioData == null || maxLength == 0) {
            audioData = null;
        } else if (audioData.length > maxLength) {
            byte[] trimmed = new byte[maxLength];
            System.arraycopy(audioData, 0, trimmed, 0, maxLength);
            audioData = trimmed;
        }
    }
}
