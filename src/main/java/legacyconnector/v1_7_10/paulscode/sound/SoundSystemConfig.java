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
    private static int numberStreamingBuffers = 3;
    private static int streamingBufferSize = 131072;
    private static int maxFileSize = 268435456;
    private static int fileChunkSize = 1048576;
    private static SoundSystemLogger logger;

    /** The lock paulscode's threads (and mods touching the library from their own) synchronise on. */
    public static final Object THREAD_SYNC = new Object();

    public static synchronized void setNumberStreamingBuffers(int number) {
        numberStreamingBuffers = number;
    }

    public static synchronized int getNumberStreamingBuffers() {
        return numberStreamingBuffers;
    }

    /** Bytes of audio per streaming buffer (default 128 KiB). */
    public static synchronized void setStreamingBufferSize(int size) {
        streamingBufferSize = size;
    }

    public static synchronized int getStreamingBufferSize() {
        return streamingBufferSize;
    }

    /** The largest file the library loads whole (default 256 MiB). */
    public static synchronized void setMaxFileSize(int size) {
        maxFileSize = size;
    }

    public static synchronized int getMaxFileSize() {
        return maxFileSize;
    }

    /** How many bytes the library reads from a file at a time (default 1 MiB). */
    public static synchronized void setFileChunkSize(int size) {
        fileChunkSize = size;
    }

    public static synchronized int getFileChunkSize() {
        return fileChunkSize;
    }

    /** The library's log sink (mods replace it to capture or silence the lines). */
    public static synchronized void setLogger(SoundSystemLogger sink) {
        logger = sink;
    }

    public static synchronized SoundSystemLogger getLogger() {
        return logger;
    }

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
