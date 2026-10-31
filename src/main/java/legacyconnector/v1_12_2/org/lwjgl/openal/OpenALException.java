package legacyconnector.v1_12_2.org.lwjgl.openal;

/** LWJGL 2's exception for an OpenAL error (LWJGL 3 reports error codes only); legacy code subclasses and catches it (OpenComputers' audio). */
public class OpenALException extends RuntimeException {
    public OpenALException() {
    }

    public OpenALException(int errorCode) {
        super("OpenAL error " + errorCode);
    }

    public OpenALException(String message) {
        super(message);
    }

    public OpenALException(String message, Throwable cause) {
        super(message, cause);
    }
}
