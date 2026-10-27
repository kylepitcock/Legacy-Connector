package legacyconnector.v1_12_2.org.lwjgl.util;

import java.io.Serializable;
import java.nio.ByteBuffer;

/** LWJGL 2's mutable RGBA colour (components kept as 0-255, as LWJGL stored them in bytes). */
public final class Color implements ReadableColor, Serializable, WritableColor {
    private byte red, green, blue, alpha;

    public Color() {
        this(0, 0, 0, 255);
    }

    public Color(int r, int g, int b) {
        this(r, g, b, 255);
    }

    public Color(byte r, byte g, byte b) {
        this(r, g, b, (byte) 255);
    }

    public Color(int r, int g, int b, int a) {
        set(r, g, b, a);
    }

    public Color(byte r, byte g, byte b, byte a) {
        set(r, g, b, a);
    }

    public Color(ReadableColor c) {
        setColor(c);
    }

    @Override
    public void set(int r, int g, int b, int a) {
        red = (byte) r;
        green = (byte) g;
        blue = (byte) b;
        alpha = (byte) a;
    }

    @Override
    public void set(byte r, byte g, byte b, byte a) {
        red = r;
        green = g;
        blue = b;
        alpha = a;
    }

    @Override
    public void set(int r, int g, int b) {
        set(r, g, b, 255);
    }

    @Override
    public void set(byte r, byte g, byte b) {
        set(r, g, b, (byte) 255);
    }

    @Override
    public int getRed() {
        return red & 255;
    }

    @Override
    public int getGreen() {
        return green & 255;
    }

    @Override
    public int getBlue() {
        return blue & 255;
    }

    @Override
    public int getAlpha() {
        return alpha & 255;
    }

    @Override
    public void setRed(int red) {
        this.red = (byte) red;
    }

    @Override
    public void setGreen(int green) {
        this.green = (byte) green;
    }

    @Override
    public void setBlue(int blue) {
        this.blue = (byte) blue;
    }

    @Override
    public void setAlpha(int alpha) {
        this.alpha = (byte) alpha;
    }

    @Override
    public void setRed(byte red) {
        this.red = red;
    }

    @Override
    public void setGreen(byte green) {
        this.green = green;
    }

    @Override
    public void setBlue(byte blue) {
        this.blue = blue;
    }

    @Override
    public void setAlpha(byte alpha) {
        this.alpha = alpha;
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
    public void writeARGB(ByteBuffer dest) {
        dest.put(alpha).put(red).put(green).put(blue);
    }

    @Override
    public void writeBGR(ByteBuffer dest) {
        dest.put(blue).put(green).put(red);
    }

    @Override
    public void writeBGRA(ByteBuffer dest) {
        dest.put(blue).put(green).put(red).put(alpha);
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
    }

    public void readARGB(ByteBuffer src) {
        alpha = src.get();
        red = src.get();
        green = src.get();
        blue = src.get();
    }

    public void readBGRA(ByteBuffer src) {
        blue = src.get();
        green = src.get();
        red = src.get();
        alpha = src.get();
    }

    public void readBGR(ByteBuffer src) {
        blue = src.get();
        green = src.get();
        red = src.get();
    }

    public void readABGR(ByteBuffer src) {
        alpha = src.get();
        blue = src.get();
        green = src.get();
        red = src.get();
    }

    @Override
    public void setColor(ReadableColor src) {
        red = src.getRedByte();
        green = src.getGreenByte();
        blue = src.getBlueByte();
        alpha = src.getAlphaByte();
    }

    /** HSB to RGB, as java.awt.Color does (AWT itself is not touched: loading it next to GLFW is unsafe on macOS). */
    public void fromHSB(float hue, float saturation, float brightness) {
        int rgb = net.minecraft.util.Mth.hsvToRgb(hue - (float) Math.floor(hue), saturation, brightness);
        set(rgb >> 16 & 255, rgb >> 8 & 255, rgb & 255);
    }

    public float[] toHSB(float[] dest) {
        int r = getRed(), g = getGreen(), b = getBlue();
        int max = Math.max(r, Math.max(g, b)), min = Math.min(r, Math.min(g, b));
        float brightness = max / 255.0F;
        float saturation = max == 0 ? 0 : (max - min) / (float) max;
        float hue = 0;
        if (saturation != 0) {
            float rc = (max - r) / (float) (max - min), gc = (max - g) / (float) (max - min), bc = (max - b) / (float) (max - min);
            hue = r == max ? bc - gc : g == max ? 2.0F + rc - bc : 4.0F + gc - rc;
            hue /= 6.0F;
            if (hue < 0) hue += 1.0F;
        }
        if (dest == null) dest = new float[3];
        dest[0] = hue;
        dest[1] = saturation;
        dest[2] = brightness;
        return dest;
    }

    @Override
    public String toString() {
        return "Color [" + getRed() + ", " + getGreen() + ", " + getBlue() + ", " + getAlpha() + "]";
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ReadableColor c && c.getRed() == getRed() && c.getGreen() == getGreen() && c.getBlue() == getBlue()
                && c.getAlpha() == getAlpha();
    }

    @Override
    public int hashCode() {
        return (red << 24) | (green << 16) | (blue << 8) | (alpha & 255);
    }
}
