package legacyconnector.v1_7_10.codechicken.lib.colour;

/** A colour packed as 0xRRGGBBAA. */
public class ColourRGBA extends Colour {
    public ColourRGBA(int colour) {
        super(colour >>> 24, colour >> 16 & 0xFF, colour >> 8 & 0xFF, colour & 0xFF);
    }

    public ColourRGBA(int r, int g, int b, int a) {
        super(r, g, b, a);
    }

    public ColourRGBA(double r, double g, double b, double a) {
        super((int) (r * 255), (int) (g * 255), (int) (b * 255), (int) (a * 255));
    }

    public ColourRGBA(Colour colour) {
        super(colour);
    }

    public int pack() {
        return rgba();
    }

    public Colour copy() {
        return new ColourRGBA(this);
    }

    public static int pack(Colour colour) {
        return colour.rgba();
    }

    /** Multiplies two packed 0xRRGGBBAA colours channel by channel. */
    public static int multiply(int c1, int c2) {
        if (c1 == 0xFFFFFFFF) return c2;
        if (c2 == 0xFFFFFFFF) return c1;
        int r = (c1 >>> 24) * (c2 >>> 24) / 255;
        int g = (c1 >> 16 & 0xFF) * (c2 >> 16 & 0xFF) / 255;
        int b = (c1 >> 8 & 0xFF) * (c2 >> 8 & 0xFF) / 255;
        int a = (c1 & 0xFF) * (c2 & 0xFF) / 255;
        return r << 24 | g << 16 | b << 8 | a;
    }
}
