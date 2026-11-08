package legacyconnector.v1_7_10.codechicken.lib.render;

import legacyconnector.v1_7_10.codechicken.lib.colour.ColourRGBA;

/** Multiplies the vertex colour by a fixed colour (0xRRGGBBAA). */
public class ColourMultiplier implements CCRenderState.IVertexOperation {
    public static final int operationIndex = CCRenderState.registerOperation();
    private static final ColourMultiplier INSTANCE = new ColourMultiplier(-1);

    public int colour;

    public ColourMultiplier(int colour) {
        this.colour = colour;
    }

    public static ColourMultiplier instance(int colour) {
        INSTANCE.colour = colour;
        return INSTANCE;
    }

    public boolean load() {
        if (colour == -1) return false;
        CCRenderState.hasColour = true;
        return true;
    }

    public void operate() {
        CCRenderState.setColour(ColourRGBA.multiply(CCRenderState.colour, colour));
    }

    public int operationID() {
        return operationIndex;
    }
}
