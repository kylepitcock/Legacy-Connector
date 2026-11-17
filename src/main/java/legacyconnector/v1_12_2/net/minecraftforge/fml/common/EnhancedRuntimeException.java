package legacyconnector.v1_12_2.net.minecraftforge.fml.common;

import java.io.PrintStream;
import java.io.PrintWriter;

/** FML's exception base: it prints extra detail before its stack trace. Subclasses supply {@link #printStackTrace(WrappedPrintStream)}. */
public abstract class EnhancedRuntimeException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public EnhancedRuntimeException() {
        super();
    }

    public EnhancedRuntimeException(String message) {
        super(message);
    }

    public EnhancedRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }

    public EnhancedRuntimeException(Throwable cause) {
        super(cause);
    }

    protected abstract void printStackTrace(WrappedPrintStream stream);

    @Override
    public void printStackTrace(final PrintStream s) {
        printStackTrace(new WrappedPrintStream() {
            @Override
            public void println(String line) {
                s.println(line);
            }
        });
        super.printStackTrace(s);
    }

    @Override
    public void printStackTrace(final PrintWriter s) {
        printStackTrace(new WrappedPrintStream() {
            @Override
            public void println(String line) {
                s.println(line);
            }
        });
        super.printStackTrace(s);
    }

    /** A line sink the exception prints its detail to. */
    public abstract static class WrappedPrintStream {
        public abstract void println(String line);
    }
}
