package legacyconnector.v1_12_2.org.lwjgl.openal;

/** LWJGL 2's OpenAL context object; LWJGL 3 passes the native handle itself. */
public final class ALCcontext {
    final long context;

    ALCcontext(long context) {
        this.context = context;
    }

    /** The wrapper of a native handle (null for 0). */
    static ALCcontext of(long handle) {
        return handle == 0L ? null : new ALCcontext(handle);
    }

    public boolean isValid() {
        return context != 0L;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ALCcontext c && c.context == context;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(context);
    }
}
