package legacyconnector.v1_7_10.org.lwjgl.opengl;

/**
 * Legacy {@code org.lwjgl.opengl.GL32}: the sync objects, which LWJGL 2 wrapped in {@link GLSync} (LWJGL 3 returns the pointer, so the class
 * cannot extend it). Relocated for the 1.4.7 to 1.7.10 family only.
 */
public class GL32 {
    public static final int GL_SYNC_GPU_COMMANDS_COMPLETE = 0x9117;
    public static final int GL_SYNC_FLUSH_COMMANDS_BIT = 0x1;
    public static final int GL_ALREADY_SIGNALED = 0x911A;
    public static final int GL_TIMEOUT_EXPIRED = 0x911B;
    public static final int GL_CONDITION_SATISFIED = 0x911C;
    public static final int GL_WAIT_FAILED = 0x911D;

    protected GL32() {
    }

    public static GLSync glFenceSync(int condition, int flags) {
        return new GLSync(org.lwjgl.opengl.GL32.glFenceSync(condition, flags));
    }

    public static boolean glIsSync(GLSync sync) {
        return sync != null && org.lwjgl.opengl.GL32.glIsSync(sync.getPointer());
    }

    public static void glDeleteSync(GLSync sync) {
        if (sync != null) org.lwjgl.opengl.GL32.glDeleteSync(sync.getPointer());
    }

    public static int glClientWaitSync(GLSync sync, int flags, long timeout) {
        return org.lwjgl.opengl.GL32.glClientWaitSync(sync.getPointer(), flags, timeout);
    }

    public static void glWaitSync(GLSync sync, int flags, long timeout) {
        org.lwjgl.opengl.GL32.glWaitSync(sync.getPointer(), flags, timeout);
    }
}
