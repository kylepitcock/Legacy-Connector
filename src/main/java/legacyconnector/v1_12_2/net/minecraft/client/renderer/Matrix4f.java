package legacyconnector.v1_12_2.net.minecraft.client.renderer;

/** 1.12.2's matrix of the shader and camera code: LWJGL 2's Matrix4f filled row by row from an array, or all zero. */
public class Matrix4f extends legacyconnector.v1_12_2.org.lwjgl.util.vector.Matrix4f {
    public Matrix4f(float[] values) {
        m00 = values[0];
        m01 = values[1];
        m02 = values[2];
        m03 = values[3];
        m10 = values[4];
        m11 = values[5];
        m12 = values[6];
        m13 = values[7];
        m20 = values[8];
        m21 = values[9];
        m22 = values[10];
        m23 = values[11];
        m30 = values[12];
        m31 = values[13];
        m32 = values[14];
        m33 = values[15];
    }

    public Matrix4f() {
        m00 = m01 = m02 = m03 = 0.0F;
        m10 = m11 = m12 = m13 = 0.0F;
        m20 = m21 = m22 = m23 = 0.0F;
        m30 = m31 = m32 = m33 = 0.0F;
    }
}
