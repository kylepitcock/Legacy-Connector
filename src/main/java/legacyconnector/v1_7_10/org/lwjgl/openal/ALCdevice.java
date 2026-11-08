package legacyconnector.v1_7_10.org.lwjgl.openal;

/** LWJGL 2's OpenAL device object; LWJGL 3 passes the native handle itself. */
public final class ALCdevice {
    final long device;

    ALCdevice(long device) {
        this.device = device;
    }

    /** The wrapper of a native handle (null for 0). */
    static ALCdevice of(long handle) {
        return handle == 0L ? null : new ALCdevice(handle);
    }

    public boolean isValid() {
        return device != 0L;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ALCdevice d && d.device == device;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(device);
    }
}
