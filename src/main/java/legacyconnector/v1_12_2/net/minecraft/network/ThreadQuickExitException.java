package legacyconnector.v1_12_2.net.minecraft.network;

/** Thrown to leave a packet handler that has handed its work to the main thread. */
public final class ThreadQuickExitException extends RuntimeException {
    public static final ThreadQuickExitException INSTANCE = new ThreadQuickExitException();

    private ThreadQuickExitException() {
        setStackTrace(new StackTraceElement[0]);
    }

    @Override
    public synchronized Throwable fillInStackTrace() {
        setStackTrace(new StackTraceElement[0]);
        return this;
    }
}
