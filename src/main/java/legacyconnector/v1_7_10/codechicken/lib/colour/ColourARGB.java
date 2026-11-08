package legacyconnector.v1_7_10.codechicken.lib.colour;

/** A colour packed as 0xAARRGGBB. */
public class ColourARGB extends Colour {
    public ColourARGB(int colour) {
        super(colour >> 16 & 0xFF, colour >> 8 & 0xFF, colour & 0xFF, colour >>> 24);
    }

    public ColourARGB(int a, int r, int g, int b) {
        super(r, g, b, a);
    }

    public ColourARGB(Colour colour) {
        super(colour);
    }

    public int pack() {
        return argb();
    }

    public Colour copy() {
        return new ColourARGB(this);
    }
}
