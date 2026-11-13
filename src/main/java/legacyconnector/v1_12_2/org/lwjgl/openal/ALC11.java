package legacyconnector.v1_12_2.org.lwjgl.openal;

import java.nio.ByteBuffer;

/** LWJGL 2's {@code ALC11} (audio capture): the calls over the {@link ALCdevice} object, mapped onto LWJGL 3's long-handle API. */
public final class ALC11 {
    private ALC11() {}

    public static final int ALC_CAPTURE_DEVICE_SPECIFIER = 0x310;
    public static final int ALC_CAPTURE_DEFAULT_DEVICE_SPECIFIER = 0x311;
    public static final int ALC_CAPTURE_SAMPLES = 0x312;
    public static final int ALC_MONO_SOURCES = 0x1010;
    public static final int ALC_STEREO_SOURCES = 0x1011;

    private static long handle(ALCdevice d) {
        return d == null ? 0L : d.device;
    }

    /** Opens a capture device; null when the device cannot be opened (LWJGL 2 threw an exception for that, callers check both). */
    public static ALCdevice alcCaptureOpenDevice(String devicename, int frequency, int format, int buffersize) {
        return ALCdevice.of(org.lwjgl.openal.ALC11.alcCaptureOpenDevice(devicename, frequency, format, buffersize));
    }

    public static boolean alcCaptureCloseDevice(ALCdevice device) {
        return org.lwjgl.openal.ALC11.alcCaptureCloseDevice(handle(device));
    }

    public static void alcCaptureStart(ALCdevice device) {
        org.lwjgl.openal.ALC11.alcCaptureStart(handle(device));
    }

    public static void alcCaptureStop(ALCdevice device) {
        org.lwjgl.openal.ALC11.alcCaptureStop(handle(device));
    }

    public static void alcCaptureSamples(ALCdevice device, ByteBuffer buffer, int samples) {
        org.lwjgl.openal.ALC11.alcCaptureSamples(handle(device), buffer, samples);
    }
}
