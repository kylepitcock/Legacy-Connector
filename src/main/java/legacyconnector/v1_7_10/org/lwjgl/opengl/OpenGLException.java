package legacyconnector.v1_7_10.org.lwjgl.opengl;

/** LWJGL 2's exception for a GL error; legacy code catches it around optional rendering paths. */
public class OpenGLException extends RuntimeException {
    public OpenGLException() {
    }

    public OpenGLException(int errorCode) {
        super("OpenGL error " + errorCode);
    }

    public OpenGLException(String message) {
        super(message);
    }
}
