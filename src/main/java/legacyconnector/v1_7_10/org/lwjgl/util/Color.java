package legacyconnector.v1_7_10.org.lwjgl.util;

import java.io.Serializable;
import java.nio.ByteBuffer;

/** LWJGL 2's colour: four 8-bit channels (alpha defaults to opaque). */
public final class Color implements ReadableColor, Serializable {
    private static final long serialVersionUID = 1L;

    private byte red;
    private byte green;
    private byte blue;
    private byte alpha;

    public Color() {
        this(0, 0, 0, 255);
    }

    public Color(int r, int g, int b) {
        this(r, g, b, 255);
    }

    public Color(byte r, byte g, byte b) {
        this(r & 0xFF, g & 0xFF, b & 0xFF, 255);
    }

    public Color(int r, int g, int b, int a) {
        set(r, g, b, a);
    }

    public Color(byte r, byte g, byte b, byte a) {
        this(r & 0xFF, g & 0xFF, b & 0xFF, a & 0xFF);
    }

    public Color(ReadableColor c) {
        setColor(c);
    }

    public void set(int r, int g, int b, int a) {
        red = (byte) r;
        green = (byte) g;
        blue = (byte) b;
        alpha = (byte) a;
    }

    public void set(int r, int g, int b) {
        set(r, g, b, 255);
    }

    public void set(byte r, byte g, byte b, byte a) {
        red = r;
        green = g;
        blue = b;
        alpha = a;
    }

    public void set(byte r, byte g, byte b) {
        set(r, g, b, (byte) 255);
    }

    public void setRed(int red) {
        this.red = (byte) red;
    }

    public void setGreen(int green) {
        this.green = (byte) green;
    }

    public void setBlue(int blue) {
        this.blue = (byte) blue;
    }

    public void setAlpha(int alpha) {
        this.alpha = (byte) alpha;
    }

    public void setRed(byte red) {
        this.red = red;
    }

    public void setGreen(byte green) {
        this.green = green;
    }

    public void setBlue(byte blue) {
        this.blue = blue;
    }

    public void setAlpha(byte alpha) {
        this.alpha = alpha;
    }

    public void setColor(ReadableColor src) {
        red = src.getRedByte();
        green = src.getGreenByte();
        blue = src.getBlueByte();
        alpha = src.getAlphaByte();
    }

    @Override
    public int getRed() {
        return red & 0xFF;
    }

    @Override
    public int getGreen() {
        return green & 0xFF;
    }

    @Override
    public int getBlue() {
        return blue & 0xFF;
    }

    @Override
    public int getAlpha() {
        return alpha & 0xFF;
    }

    @Override
    public byte getRedByte() {
        return red;
    }

    @Override
    public byte getGreenByte() {
        return green;
    }

    @Override
    public byte getBlueByte() {
        return blue;
    }

    @Override
    public byte getAlphaByte() {
        return alpha;
    }

    @Override
    public void writeRGBA(ByteBuffer dest) {
        dest.put(red).put(green).put(blue).put(alpha);
    }

    @Override
    public void writeRGB(ByteBuffer dest) {
        dest.put(red).put(green).put(blue);
    }

    @Override
    public void writeABGR(ByteBuffer dest) {
        dest.put(alpha).put(blue).put(green).put(red);
    }

    @Override
    public void writeBGR(ByteBuffer dest) {
        dest.put(blue).put(green).put(red);
    }

    @Override
    public void writeBGRA(ByteBuffer dest) {
        dest.put(blue).put(green).put(red).put(alpha);
    }

    @Override
    public void writeARGB(ByteBuffer dest) {
        dest.put(alpha).put(red).put(green).put(blue);
    }

    public void readRGBA(ByteBuffer src) {
        red = src.get();
        green = src.get();
        blue = src.get();
        alpha = src.get();
    }

    public void readRGB(ByteBuffer src) {
        red = src.get();
        green = src.get();
        blue = src.get();
        alpha = (byte) 255;
    }

    public void readARGB(ByteBuffer src) {
        alpha = src.get();
        red = src.get();
        green = src.get();
        blue = src.get();
    }

    public void readABGR(ByteBuffer src) {
        alpha = src.get();
        blue = src.get();
        green = src.get();
        red = src.get();
    }

    public void readBGR(ByteBuffer src) {
        blue = src.get();
        green = src.get();
        red = src.get();
        alpha = (byte) 255;
    }

    public void readBGRA(ByteBuffer src) {
        blue = src.get();
        green = src.get();
        red = src.get();
        alpha = src.get();
    }

    /** Sets the colour from hue, saturation and brightness (each 0 to 1), keeping the alpha. */
    public void fromHSB(float hue, float saturation, float brightness) {
        int rgb = java.awt.Color.HSBtoRGB(hue, saturation, brightness);
        red = (byte) (rgb >> 16);
        green = (byte) (rgb >> 8);
        blue = (byte) rgb;
    }

    /** Hue, saturation and brightness of this colour (0 to 1 each) into {@code dest} (a new array when null). */
    public float[] toHSB(float[] dest) {
        return java.awt.Color.RGBtoHSB(getRed(), getGreen(), getBlue(), dest);
    }

    @Override
    public String toString() {
        return "Color [" + getRed() + ", " + getGreen() + ", " + getBlue() + ", " + getAlpha() + "]";
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Color c && c.red == red && c.green == green && c.blue == blue && c.alpha == alpha;
    }

    @Override
    public int hashCode() {
        return red << 24 | green << 16 | blue << 8 | alpha;
    }
}
