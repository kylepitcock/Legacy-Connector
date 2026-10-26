package legacyconnector.v1_7_10.net.minecraft.client.shader;

/** A saved Tessellator draw (see Tessellator#getVertexState): 1.7.10's raw vertex data and which attributes it has. */
public class TesselatorVertexState {
    private final int[] rawBuffer;
    private final int rawBufferIndex;
    private final int vertexCount;
    private final boolean hasTexture;
    private final boolean hasBrightness;
    private final boolean hasNormals;
    private final boolean hasColor;
    /** The emulated Tessellator's own vertices, when the state came from it (restored exactly). */
    public java.util.List<double[]> mcc$vertices;

    public TesselatorVertexState(int[] rawBuffer, int rawBufferIndex, int vertexCount, boolean hasTexture, boolean hasBrightness, boolean hasNormals,
            boolean hasColor) {
        this.rawBuffer = rawBuffer;
        this.rawBufferIndex = rawBufferIndex;
        this.vertexCount = vertexCount;
        this.hasTexture = hasTexture;
        this.hasBrightness = hasBrightness;
        this.hasNormals = hasNormals;
        this.hasColor = hasColor;
    }

    public int[] getRawBuffer() {
        return rawBuffer;
    }

    public int getRawBufferIndex() {
        return rawBufferIndex;
    }

    public int getVertexCount() {
        return vertexCount;
    }

    public boolean getHasTexture() {
        return hasTexture;
    }

    public boolean getHasBrightness() {
        return hasBrightness;
    }

    public boolean getHasNormals() {
        return hasNormals;
    }

    public boolean getHasColor() {
        return hasColor;
    }
}
