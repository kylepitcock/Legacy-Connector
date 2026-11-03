package legacyconnector.v1_7_10.org.lwjgl.util.glu;

/** A {@link GLUtessellatorCallback} whose methods do nothing; subclasses override the ones they need. */
public class GLUtessellatorCallbackAdapter implements GLUtessellatorCallback {
    @Override
    public void begin(int type) {
    }

    @Override
    public void beginData(int type, Object polygonData) {
    }

    @Override
    public void edgeFlag(boolean boundaryEdge) {
    }

    @Override
    public void edgeFlagData(boolean boundaryEdge, Object polygonData) {
    }

    @Override
    public void vertex(Object vertexData) {
    }

    @Override
    public void vertexData(Object vertexData, Object polygonData) {
    }

    @Override
    public void end() {
    }

    @Override
    public void endData(Object polygonData) {
    }

    @Override
    public void combine(double[] coords, Object[] data, float[] weight, Object[] outData) {
    }

    @Override
    public void combineData(double[] coords, Object[] data, float[] weight, Object[] outData, Object polygonData) {
    }

    @Override
    public void error(int errnum) {
    }

    @Override
    public void errorData(int errnum, Object polygonData) {
    }
}
