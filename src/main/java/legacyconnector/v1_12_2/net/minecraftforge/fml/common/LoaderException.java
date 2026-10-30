package legacyconnector.v1_12_2.net.minecraftforge.fml.common;

/** A failure of mod loading (thrown by mods to stop the game with a message). */
public class LoaderException extends RuntimeException {
    private static final long serialVersionUID = -5675297950958861378L;

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
}
