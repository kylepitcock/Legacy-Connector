package legacyconnector.v1_7_10.org.lwjgl.opengl;

/** Legacy {@code org.lwjgl.opengl.ARBSync}: the same sync objects as {@link GL32}. */
public class ARBSync {
    public static final int GL_SYNC_GPU_COMMANDS_COMPLETE = GL32.GL_SYNC_GPU_COMMANDS_COMPLETE;
    public static final int GL_SYNC_FLUSH_COMMANDS_BIT = GL32.GL_SYNC_FLUSH_COMMANDS_BIT;
    public static final int GL_ALREADY_SIGNALED = GL32.GL_ALREADY_SIGNALED;
    public static final int GL_TIMEOUT_EXPIRED = GL32.GL_TIMEOUT_EXPIRED;
    public static final int GL_CONDITION_SATISFIED = GL32.GL_CONDITION_SATISFIED;
    public static final int GL_WAIT_FAILED = GL32.GL_WAIT_FAILED;

    protected ARBSync() {
    }

    public static GLSync glFenceSync(int condition, int flags) {
        return GL32.glFenceSync(condition, flags);
    }

    public static boolean glIsSync(GLSync sync) {
        return GL32.glIsSync(sync);
    }

    public static void glDeleteSync(GLSync sync) {
        GL32.glDeleteSync(sync);
    }

    public static int glClientWaitSync(GLSync sync, int flags, long timeout) {
        return GL32.glClientWaitSync(sync, flags, timeout);
    }

    public static void glWaitSync(GLSync sync, int flags, long timeout) {
        GL32.glWaitSync(sync, flags, timeout);
    }
}
