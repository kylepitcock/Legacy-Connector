package legacyconnector.v1_7_10.org.lwjgl.opengl;

/** Legacy {@code org.lwjgl.opengl.GLSync}: LWJGL 2's wrapper of a sync object; LWJGL 3 passes the pointer itself. */
public final class GLSync {
    private final long pointer;

    public GLSync(long pointer) {
        this.pointer = pointer;
    }

    public long getPointer() {
        return pointer;
    }

    public boolean isValid() {
        return pointer != 0L;
    }
}
