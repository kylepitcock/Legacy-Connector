package legacyconnector.v1_7_10.org.lwjgl.util;

import java.nio.ByteBuffer;

/** LWJGL 2's read-only colour: four 8-bit channels. */
public interface ReadableColor {
    ReadableColor RED = new Color(255, 0, 0, 255);
    ReadableColor ORANGE = new Color(255, 128, 0, 255);
    ReadableColor YELLOW = new Color(255, 255, 0, 255);
    ReadableColor GREEN = new Color(0, 255, 0, 255);
    ReadableColor CYAN = new Color(0, 255, 255, 255);
    ReadableColor BLUE = new Color(0, 0, 255, 255);
    ReadableColor PURPLE = new Color(255, 0, 255, 255);
    ReadableColor WHITE = new Color(255, 255, 255, 255);
    ReadableColor BLACK = new Color(0, 0, 0, 255);
    ReadableColor LTGREY = new Color(192, 192, 192, 255);
    ReadableColor DKGREY = new Color(64, 64, 64, 255);
    ReadableColor GREY = new Color(128, 128, 128, 255);

    int getRed();

    int getGreen();

    int getBlue();

    int getAlpha();

    byte getRedByte();

    byte getGreenByte();

    byte getBlueByte();

    byte getAlphaByte();

    void writeRGBA(ByteBuffer dest);

    void writeRGB(ByteBuffer dest);

    void writeABGR(ByteBuffer dest);

    void writeBGR(ByteBuffer dest);

    void writeBGRA(ByteBuffer dest);

    void writeARGB(ByteBuffer dest);
}
