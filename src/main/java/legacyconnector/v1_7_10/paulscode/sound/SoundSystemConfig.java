package legacyconnector.v1_7_10.paulscode.sound;

/**
 * paulscode's global sound settings. Channel counts are kept for mods that read them back; the modern engine sizes its
 * own channel pools from the audio device, and legacy sources take their channels from it.
 */
public class SoundSystemConfig {
    public static final int ATTENUATION_NONE = 0;
    public static final int ATTENUATION_ROLLOFF = 1;
    public static final int ATTENUATION_LINEAR = 2;
    private static int numberNormalChannels = 28;
    private static int numberStreamingChannels = 4;
    private static float defaultRolloff = 0.03F;
    private static float defaultFadeDistance = 1000.0F;
    private static float masterGain = 1.0F;

    public static synchronized void setNumberNormalChannels(int number) {
        numberNormalChannels = number;
    }

    public static synchronized int getNumberNormalChannels() {
        return numberNormalChannels;
    }

    public static synchronized void setNumberStreamingChannels(int number) {
        numberStreamingChannels = number;
    }

    public static synchronized int getNumberStreamingChannels() {
        return numberStreamingChannels;
    }

    public static synchronized void setDefaultRolloff(float rolloff) {
        defaultRolloff = rolloff;
    }

    public static synchronized float getDefaultRolloff() {
        return defaultRolloff;
    }

    public static synchronized void setDefaultFadeDistance(float distance) {
        defaultFadeDistance = distance;
    }

    public static synchronized float getDefaultFadeDistance() {
        return defaultFadeDistance;
    }

    public static synchronized void setMasterGain(float gain) {
        masterGain = gain;
    }

    public static synchronized float getMasterGain() {
        return masterGain;
    }
}
