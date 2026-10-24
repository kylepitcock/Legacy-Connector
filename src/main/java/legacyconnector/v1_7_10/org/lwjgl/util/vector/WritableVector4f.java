package legacyconnector.v1_7_10.org.lwjgl.util.vector;

public interface WritableVector4f extends WritableVector3f {
    void set(float x, float y, float z, float w);

    void setW(float w);
}
