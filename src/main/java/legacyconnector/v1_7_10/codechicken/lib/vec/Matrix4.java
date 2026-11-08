package legacyconnector.v1_7_10.codechicken.lib.vec;

/** A 4x4 double matrix (m01 = row 0, column 1), the form CodeChickenLib transformations compile to. */
public class Matrix4 {
    public double m00, m01, m02, m03;
    public double m10, m11, m12, m13;
    public double m20, m21, m22, m23;
    public double m30, m31, m32, m33;

    public Matrix4() {
        m00 = m11 = m22 = m33 = 1;
    }

    public Matrix4(Matrix4 o) {
        set(o);
    }

    public Matrix4(double m00, double m01, double m02, double m03, double m10, double m11, double m12, double m13,
            double m20, double m21, double m22, double m23, double m30, double m31, double m32, double m33) {
        fromRowMajor(new double[] {m00, m01, m02, m03, m10, m11, m12, m13, m20, m21, m22, m23, m30, m31, m32, m33});
    }

    public Matrix4 set(Matrix4 o) {
        return fromRowMajor(o.toArrayRowMajor());
    }

    public Matrix4 copy() {
        return new Matrix4(this);
    }

    public Matrix4 setIdentity() {
        return set(new Matrix4());
    }

    /** this = this * translation: the translation acts on vectors first. */
    public Matrix4 translate(Vector3 v) {
        m03 += m00 * v.x + m01 * v.y + m02 * v.z;
        m13 += m10 * v.x + m11 * v.y + m12 * v.z;
        m23 += m20 * v.x + m21 * v.y + m22 * v.z;
        m33 += m30 * v.x + m31 * v.y + m32 * v.z;
        return this;
    }

    public Matrix4 scale(Vector3 v) {
        m00 *= v.x; m10 *= v.x; m20 *= v.x; m30 *= v.x;
        m01 *= v.y; m11 *= v.y; m21 *= v.y; m31 *= v.y;
        m02 *= v.z; m12 *= v.z; m22 *= v.z; m32 *= v.z;
        return this;
    }

    public Matrix4 rotate(double angle, Vector3 axis) {
        double c = Math.cos(angle);
        double s = Math.sin(angle);
        double t = 1 - c;
        Vector3 a = axis.copy().normalize();
        double x = a.x, y = a.y, z = a.z;
        return multiply(new Matrix4(
                t * x * x + c, t * x * y - s * z, t * x * z + s * y, 0,
                t * x * y + s * z, t * y * y + c, t * y * z - s * x, 0,
                t * x * z - s * y, t * y * z + s * x, t * z * z + c, 0,
                0, 0, 0, 1));
    }

    public Matrix4 rotate(double angle, double x, double y, double z) {
        return rotate(angle, new Vector3(x, y, z));
    }

    /** this = this * o. */
    public Matrix4 multiply(Matrix4 o) {
        double[] a = toArrayRowMajor();
        double[] b = o.toArrayRowMajor();
        double[] r = new double[16];
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                double sum = 0;
                for (int k = 0; k < 4; k++) sum += a[i * 4 + k] * b[k * 4 + j];
                r[i * 4 + j] = sum;
            }
        }
        return fromRowMajor(r);
    }

    /** Transforms the vector (with translation). */
    public void apply(Vector3 v) {
        double x = m00 * v.x + m01 * v.y + m02 * v.z + m03;
        double y = m10 * v.x + m11 * v.y + m12 * v.z + m13;
        double z = m20 * v.x + m21 * v.y + m22 * v.z + m23;
        v.set(x, y, z);
    }

    /** Transforms a direction (no translation). */
    public void applyN(Vector3 v) {
        double x = m00 * v.x + m01 * v.y + m02 * v.z;
        double y = m10 * v.x + m11 * v.y + m12 * v.z;
        double z = m20 * v.x + m21 * v.y + m22 * v.z;
        v.set(x, y, z);
    }

    private Matrix4 fromRowMajor(double[] r) {
        m00 = r[0]; m01 = r[1]; m02 = r[2]; m03 = r[3];
        m10 = r[4]; m11 = r[5]; m12 = r[6]; m13 = r[7];
        m20 = r[8]; m21 = r[9]; m22 = r[10]; m23 = r[11];
        m30 = r[12]; m31 = r[13]; m32 = r[14]; m33 = r[15];
        return this;
    }

    public double[] toArrayRowMajor() {
        return new double[] {m00, m01, m02, m03, m10, m11, m12, m13, m20, m21, m22, m23, m30, m31, m32, m33};
    }

    /** The layout OpenGL expects. */
    public double[] toArrayColumnMajor() {
        return new double[] {m00, m10, m20, m30, m01, m11, m21, m31, m02, m12, m22, m32, m03, m13, m23, m33};
    }

    public String toString() {
        return "Matrix4" + java.util.Arrays.toString(toArrayRowMajor());
    }
}
