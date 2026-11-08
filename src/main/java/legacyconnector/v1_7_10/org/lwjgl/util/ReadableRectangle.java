package legacyconnector.v1_7_10.org.lwjgl.util;

/** LWJGL 2's read-only rectangle. */
public interface ReadableRectangle {
    int getX();

    int getY();

    int getWidth();

    int getHeight();

    void getBounds(Rectangle dest);
}
