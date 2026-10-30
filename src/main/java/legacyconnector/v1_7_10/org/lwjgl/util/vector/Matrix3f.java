package legacyconnector.v1_7_10.org.lwjgl.util.vector;

import java.nio.FloatBuffer;

/** LWJGL 2's 3x3 matrix (column-major fields mCR: column C, row R), as LWJGL 2.9 had it. */
public class Matrix3f extends Matrix {
    public float m00, m01, m02, m10, m11, m12, m20, m21, m22;

    public Matrix3f() {
        setIdentity();
    }

    public Matrix3f load(Matrix3f src) {
        return load(src, this);
    }

    public static Matrix3f load(Matrix3f src, Matrix3f dest) {
        if (dest == null) dest = new Matrix3f();
        dest.m00 = src.m00;
        dest.m10 = src.m10;
        dest.m20 = src.m20;
        dest.m01 = src.m01;
        dest.m11 = src.m11;
        dest.m21 = src.m21;
        dest.m02 = src.m02;
        dest.m12 = src.m12;
        dest.m22 = src.m22;
        return dest;
    }

    @Override
    public Matrix load(FloatBuffer buf) {
        m00 = buf.get();
        m01 = buf.get();
        m02 = buf.get();
        m10 = buf.get();
        m11 = buf.get();
        m12 = buf.get();
        m20 = buf.get();
        m21 = buf.get();
        m22 = buf.get();
        return this;
    }

    @Override
    public Matrix loadTranspose(FloatBuffer buf) {
        m00 = buf.get();
        m10 = buf.get();
        m20 = buf.get();
        m01 = buf.get();
        m11 = buf.get();
        m21 = buf.get();
        m02 = buf.get();
        m12 = buf.get();
        m22 = buf.get();
        return this;
    }

    @Override
    public Matrix store(FloatBuffer buf) {
        buf.put(m00).put(m01).put(m02).put(m10).put(m11).put(m12).put(m20).put(m21).put(m22);
        return this;
    }

    @Override
    public Matrix storeTranspose(FloatBuffer buf) {
        buf.put(m00).put(m10).put(m20).put(m01).put(m11).put(m21).put(m02).put(m12).put(m22);
        return this;
    }

    public static Matrix3f add(Matrix3f left, Matrix3f right, Matrix3f dest) {
        if (dest == null) dest = new Matrix3f();
        dest.m00 = left.m00 + right.m00;
        dest.m01 = left.m01 + right.m01;
        dest.m02 = left.m02 + right.m02;
        dest.m10 = left.m10 + right.m10;
        dest.m11 = left.m11 + right.m11;
        dest.m12 = left.m12 + right.m12;
        dest.m20 = left.m20 + right.m20;
        dest.m21 = left.m21 + right.m21;
        dest.m22 = left.m22 + right.m22;
        return dest;
    }

    public static Matrix3f sub(Matrix3f left, Matrix3f right, Matrix3f dest) {
        if (dest == null) dest = new Matrix3f();
        dest.m00 = left.m00 - right.m00;
        dest.m01 = left.m01 - right.m01;
        dest.m02 = left.m02 - right.m02;
        dest.m10 = left.m10 - right.m10;
        dest.m11 = left.m11 - right.m11;
        dest.m12 = left.m12 - right.m12;
        dest.m20 = left.m20 - right.m20;
        dest.m21 = left.m21 - right.m21;
        dest.m22 = left.m22 - right.m22;
        return dest;
    }

    public static Matrix3f mul(Matrix3f left, Matrix3f right, Matrix3f dest) {
        if (dest == null) dest = new Matrix3f();
        float m00 = left.m00 * right.m00 + left.m10 * right.m01 + left.m20 * right.m02;
        float m01 = left.m01 * right.m00 + left.m11 * right.m01 + left.m21 * right.m02;
        float m02 = left.m02 * right.m00 + left.m12 * right.m01 + left.m22 * right.m02;
        float m10 = left.m00 * right.m10 + left.m10 * right.m11 + left.m20 * right.m12;
        float m11 = left.m01 * right.m10 + left.m11 * right.m11 + left.m21 * right.m12;
        float m12 = left.m02 * right.m10 + left.m12 * right.m11 + left.m22 * right.m12;
        float m20 = left.m00 * right.m20 + left.m10 * right.m21 + left.m20 * right.m22;
        float m21 = left.m01 * right.m20 + left.m11 * right.m21 + left.m21 * right.m22;
        float m22 = left.m02 * right.m20 + left.m12 * right.m21 + left.m22 * right.m22;
        dest.m00 = m00;
        dest.m01 = m01;
        dest.m02 = m02;
        dest.m10 = m10;
        dest.m11 = m11;
        dest.m12 = m12;
        dest.m20 = m20;
        dest.m21 = m21;
        dest.m22 = m22;
        return dest;
    }

    public static Vector3f transform(Matrix3f left, Vector3f right, Vector3f dest) {
        if (dest == null) dest = new Vector3f();
        float x = left.m00 * right.x + left.m10 * right.y + left.m20 * right.z;
        float y = left.m01 * right.x + left.m11 * right.y + left.m21 * right.z;
        float z = left.m02 * right.x + left.m12 * right.y + left.m22 * right.z;
        dest.x = x;
        dest.y = y;
        dest.z = z;
        return dest;
    }

    @Override
    public Matrix transpose() {
        return transpose(this, this);
    }

    public Matrix3f transpose(Matrix3f dest) {
        return transpose(this, dest);
    }

    public static Matrix3f transpose(Matrix3f src, Matrix3f dest) {
        if (dest == null) dest = new Matrix3f();
        float m00 = src.m00, m01 = src.m10, m02 = src.m20, m10 = src.m01, m11 = src.m11, m12 = src.m21, m20 = src.m02, m21 = src.m12, m22 = src.m22;
        dest.m00 = m00;
        dest.m01 = m01;
        dest.m02 = m02;
        dest.m10 = m10;
        dest.m11 = m11;
        dest.m12 = m12;
        dest.m20 = m20;
        dest.m21 = m21;
        dest.m22 = m22;
        return dest;
    }

    @Override
    public float determinant() {
        return m00 * (m11 * m22 - m12 * m21) + m01 * (m12 * m20 - m10 * m22) + m02 * (m10 * m21 - m11 * m20);
    }

    @Override
    public String toString() {
        return m00 + " " + m10 + " " + m20 + " \n" + m01 + " " + m11 + " " + m21 + " \n" + m02 + " " + m12 + " " + m22 + " \n";
    }

    @Override
    public Matrix invert() {
        return invert(this, this);
    }

    public static Matrix3f invert(Matrix3f src, Matrix3f dest) {
        float determinant = src.determinant();
        if (determinant == 0) return null;
        if (dest == null) dest = new Matrix3f();
        float inv = 1f / determinant;
        float t00 = src.m11 * src.m22 - src.m12 * src.m21;
        float t01 = -src.m10 * src.m22 + src.m12 * src.m20;
        float t02 = src.m10 * src.m21 - src.m11 * src.m20;
        float t10 = -src.m01 * src.m22 + src.m02 * src.m21;
        float t11 = src.m00 * src.m22 - src.m02 * src.m20;
        float t12 = -src.m00 * src.m21 + src.m01 * src.m20;
        float t20 = src.m01 * src.m12 - src.m02 * src.m11;
        float t21 = -src.m00 * src.m12 + src.m02 * src.m10;
        float t22 = src.m00 * src.m11 - src.m01 * src.m10;
        dest.m00 = t00 * inv;
        dest.m11 = t11 * inv;
        dest.m22 = t22 * inv;
        dest.m01 = t10 * inv;
        dest.m10 = t01 * inv;
        dest.m20 = t02 * inv;
        dest.m02 = t20 * inv;
        dest.m12 = t21 * inv;
        dest.m21 = t12 * inv;
        return dest;
    }

    @Override
    public Matrix negate() {
        return negate(this);
    }

    public Matrix3f negate(Matrix3f dest) {
        return negate(this, dest);
    }

    public static Matrix3f negate(Matrix3f src, Matrix3f dest) {
        if (dest == null) dest = new Matrix3f();
        dest.m00 = -src.m00;
        dest.m01 = -src.m02;
        dest.m02 = -src.m01;
        dest.m10 = -src.m10;
        dest.m11 = -src.m12;
        dest.m12 = -src.m11;
        dest.m20 = -src.m20;
        dest.m21 = -src.m22;
        dest.m22 = -src.m21;
        return dest;
    }

    @Override
    public Matrix setIdentity() {
        return setIdentity(this);
    }

    public static Matrix3f setIdentity(Matrix3f m) {
        m.m00 = 1.0f;
        m.m01 = 0.0f;
        m.m02 = 0.0f;
        m.m10 = 0.0f;
        m.m11 = 1.0f;
        m.m12 = 0.0f;
        m.m20 = 0.0f;
        m.m21 = 0.0f;
        m.m22 = 1.0f;
        return m;
    }

    @Override
    public Matrix setZero() {
        return setZero(this);
    }

    public static Matrix3f setZero(Matrix3f m) {
        m.m00 = m.m01 = m.m02 = m.m10 = m.m11 = m.m12 = m.m20 = m.m21 = m.m22 = 0.0f;
        return m;
    }
}
