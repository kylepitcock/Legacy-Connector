package legacyconnector.v1_7_10.org.lwjgl.util.glu;

/** Receiver of the triangles a {@link GLUtessellator} produces. */
public interface GLUtessellatorCallback {
    void begin(int type);

    void beginData(int type, Object polygonData);

    void edgeFlag(boolean boundaryEdge);

    void edgeFlagData(boolean boundaryEdge, Object polygonData);

    void vertex(Object vertexData);

    void vertexData(Object vertexData, Object polygonData);

    void end();

    void endData(Object polygonData);

    void combine(double[] coords, Object[] data, float[] weight, Object[] outData);

    void combineData(double[] coords, Object[] data, float[] weight, Object[] outData, Object polygonData);

    void error(int errnum);

    void errorData(int errnum, Object polygonData);
}
