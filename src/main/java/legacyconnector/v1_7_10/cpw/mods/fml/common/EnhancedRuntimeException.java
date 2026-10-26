package legacyconnector.v1_7_10.cpw.mods.fml.common;

import java.io.PrintStream;
import java.io.PrintWriter;

/** FML's loading exceptions: their stack trace starts with a readable report the subclass writes. */
public abstract class EnhancedRuntimeException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public EnhancedRuntimeException() {
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
    public void printStackTrace(PrintWriter s) {
        printStackTrace(new WrappedPrintStream() {
            @Override
            public void println(String line) {
                s.println(line);
            }
        });
        super.printStackTrace(s);
    }

    @Override
    public void printStackTrace(PrintStream s) {
        printStackTrace(new WrappedPrintStream() {
            @Override
            public void println(String line) {
                s.println(line);
            }
        });
        super.printStackTrace(s);
    }

    public abstract static class WrappedPrintStream {
        public abstract void println(String line);
    }
}
