package legacyconnector.v1_7_10.org.lwjgl.openal;

import java.nio.IntBuffer;

/**
 * LWJGL 2's {@code ALC10}: device and context calls over the object handles {@link ALCdevice} and {@link ALCcontext}, mapped onto LWJGL 3's
 * own long-handle API. The constants are inlined into mods when they compile; they are kept for reflection.
 */
public final class ALC10 {
    private ALC10() {}

    public static final int ALC_INVALID = -1;
    public static final int ALC_FALSE = 0;
    public static final int ALC_TRUE = 1;
    public static final int ALC_FREQUENCY = 0x1007;
    public static final int ALC_REFRESH = 0x1008;
    public static final int ALC_SYNC = 0x1009;
    public static final int ALC_MONO_SOURCES = 0x1010;
    public static final int ALC_STEREO_SOURCES = 0x1011;
    public static final int ALC_NO_ERROR = 0;
    public static final int ALC_INVALID_DEVICE = 0xA001;
    public static final int ALC_INVALID_CONTEXT = 0xA002;
    public static final int ALC_INVALID_ENUM = 0xA003;
    public static final int ALC_INVALID_VALUE = 0xA004;
    public static final int ALC_OUT_OF_MEMORY = 0xA005;
    public static final int ALC_DEFAULT_DEVICE_SPECIFIER = 0x1004;
    public static final int ALC_DEVICE_SPECIFIER = 0x1005;
    public static final int ALC_EXTENSIONS = 0x1006;
    public static final int ALC_MAJOR_VERSION = 0x1000;
    public static final int ALC_MINOR_VERSION = 0x1001;
    public static final int ALC_ATTRIBUTES_SIZE = 0x1002;
    public static final int ALC_ALL_ATTRIBUTES = 0x1003;

    private static long handle(ALCdevice d) {
        return d == null ? 0L : d.device;
    }

    private static long handle(ALCcontext c) {
        return c == null ? 0L : c.context;
    }

    public static ALCdevice alcOpenDevice(String devicename) {
        return ALCdevice.of(org.lwjgl.openal.ALC10.alcOpenDevice(devicename));
    }

    public static boolean alcCloseDevice(ALCdevice device) {
        return org.lwjgl.openal.ALC10.alcCloseDevice(handle(device));
    }

    public static ALCcontext alcCreateContext(ALCdevice device, IntBuffer attrList) {
        return ALCcontext.of(org.lwjgl.openal.ALC10.alcCreateContext(handle(device), attrList));
    }

    public static int alcMakeContextCurrent(ALCcontext context) {
        return org.lwjgl.openal.ALC10.alcMakeContextCurrent(handle(context)) ? ALC_TRUE : ALC_FALSE;
    }

    public static void alcProcessContext(ALCcontext context) {
        org.lwjgl.openal.ALC10.alcProcessContext(handle(context));
    }

    public static void alcSuspendContext(ALCcontext context) {
        org.lwjgl.openal.ALC10.alcSuspendContext(handle(context));
    }

    public static void alcDestroyContext(ALCcontext context) {
        org.lwjgl.openal.ALC10.alcDestroyContext(handle(context));
    }

    public static ALCcontext alcGetCurrentContext() {
        return ALCcontext.of(org.lwjgl.openal.ALC10.alcGetCurrentContext());
    }

    public static ALCdevice alcGetContextsDevice(ALCcontext context) {
        return ALCdevice.of(org.lwjgl.openal.ALC10.alcGetContextsDevice(handle(context)));
    }

    public static int alcGetError(ALCdevice device) {
        return org.lwjgl.openal.ALC10.alcGetError(handle(device));
    }

    public static boolean alcIsExtensionPresent(ALCdevice device, String extName) {
        return org.lwjgl.openal.ALC10.alcIsExtensionPresent(handle(device), extName);
    }

    public static int alcGetEnumValue(ALCdevice device, String enumName) {
        return org.lwjgl.openal.ALC10.alcGetEnumValue(handle(device), enumName);
    }

    public static String alcGetString(ALCdevice device, int pname) {
        return org.lwjgl.openal.ALC10.alcGetString(handle(device), pname);
    }

    public static void alcGetInteger(ALCdevice device, int pname, IntBuffer integerdata) {
        org.lwjgl.openal.ALC10.alcGetIntegerv(handle(device), pname, integerdata);
    }
}
