package legacyconnector.v1_12_2.org.lwjgl;

/** LWJGL 2's checked exception; legacy code catches it around display and input set-up. */
public class LWJGLException extends Exception {
    public LWJGLException() {
    }

    public LWJGLException(String message) {
        super(message);
    }

    public LWJGLException(String message, Throwable cause) {
        super(message, cause);
    }

    public LWJGLException(Throwable cause) {
        super(cause);
    }
}
