package legacyconnector.v1_7_10.cpw.mods.fml.common;

/** A mod could not be loaded (mods throw it to stop the game with their reason). */
public class LoaderException extends EnhancedRuntimeException {
    public static final long serialVersionUID = 1L;

    public LoaderException(Throwable wrapped) {
        super(wrapped);
    }

    public LoaderException() {
    }

    public LoaderException(String message) {
        super(message);
    }

    public LoaderException(String message, Throwable cause) {
        super(message, cause);
    }

    @Override
    protected void printStackTrace(WrappedPrintStream stream) {
    }
}
