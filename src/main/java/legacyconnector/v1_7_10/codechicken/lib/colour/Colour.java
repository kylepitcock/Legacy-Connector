package legacyconnector.v1_7_10.codechicken.lib.colour;

import legacyconnector.v1_7_10.org.lwjgl.opengl.GL11;

/** CodeChickenLib's colour: four bytes (r, g, b, a), packed in the order of the subclass. */
public abstract class Colour {
    public byte r;
    public byte g;
    public byte b;
    public byte a;

    public Colour(int r, int g, int b, int a) {
        this.r = (byte) r;
        this.g = (byte) g;
        this.b = (byte) b;
        this.a = (byte) a;
    }

    public Colour(Colour colour) {
        this(colour.r, colour.g, colour.b, colour.a);
    }

    public int rgb() {
        return (r & 0xFF) << 16 | (g & 0xFF) << 8 | (b & 0xFF);
    }

    public int argb() {
        return (a & 0xFF) << 24 | rgb();
    }

    public int rgba() {
        return (r & 0xFF) << 24 | (g & 0xFF) << 16 | (b & 0xFF) << 8 | (a & 0xFF);
    }

    /** The packed int in this colour's own byte order. */
    public abstract int pack();

    public abstract Colour copy();

    public void glColour() {
        GL11.glColor4ub(r, g, b, a);
    }

    public void glColour(int alpha) {
        GL11.glColor4ub(r, g, b, (byte) alpha);
    }

    public Colour set(Colour colour) {
        r = colour.r;
        g = colour.g;
        b = colour.b;
        a = colour.a;
        return this;
    }

    public Colour multiply(Colour colour) {
        r = (byte) ((r & 0xFF) * (colour.r & 0xFF) / 255);
        g = (byte) ((g & 0xFF) * (colour.g & 0xFF) / 255);
        b = (byte) ((b & 0xFF) * (colour.b & 0xFF) / 255);
        a = (byte) ((a & 0xFF) * (colour.a & 0xFF) / 255);
        return this;
    }

    public Colour scale(double d) {
        r = (byte) Math.min(255, (int) ((r & 0xFF) * d));
        g = (byte) Math.min(255, (int) ((g & 0xFF) * d));
        b = (byte) Math.min(255, (int) ((b & 0xFF) * d));
        a = (byte) Math.min(255, (int) ((a & 0xFF) * d));
        return this;
    }

    public Colour interpolate(Colour colour, double d) {
        r = (byte) (int) ((r & 0xFF) + ((colour.r & 0xFF) - (r & 0xFF)) * d);
        g = (byte) (int) ((g & 0xFF) + ((colour.g & 0xFF) - (g & 0xFF)) * d);
        b = (byte) (int) ((b & 0xFF) + ((colour.b & 0xFF) - (b & 0xFF)) * d);
        a = (byte) (int) ((a & 0xFF) + ((colour.a & 0xFF) - (a & 0xFF)) * d);
        return this;
    }

    public boolean equals(Object o) {
        return o instanceof Colour c && c.r == r && c.g == g && c.b == b && c.a == a;
    }

    public int hashCode() {
        return rgba();
    }
}
