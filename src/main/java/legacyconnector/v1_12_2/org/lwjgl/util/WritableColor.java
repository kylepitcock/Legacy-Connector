package legacyconnector.v1_12_2.org.lwjgl.util;

/** LWJGL 2's writable colour. */
public interface WritableColor {
    void set(int r, int g, int b, int a);

    void set(byte r, byte g, byte b, byte a);

    void set(int r, int g, int b);

    void set(byte r, byte g, byte b);

    void setRed(int red);

    void setGreen(int green);

    void setBlue(int blue);

    void setAlpha(int alpha);

    void setRed(byte red);

    void setGreen(byte green);

    void setBlue(byte blue);

    void setAlpha(byte alpha);

    void setColor(ReadableColor src);
}
