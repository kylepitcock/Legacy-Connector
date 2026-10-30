package legacyconnector.v1_12_2.org.lwjgl.util.vector;

import java.nio.FloatBuffer;

/** LWJGL 2's 2x2 matrix (Traincraft lays out its track pieces with it); a new matrix is the identity. */
public class Matrix2f extends Matrix {
    public float m00, m01, m10, m11;

    public Matrix2f() {
        setIdentity();
    }

    public Matrix2f(Matrix2f src) {
        load(src);
    }

    public Matrix2f load(Matrix2f src) {
        return load(src, this);
    }

    public static Matrix2f load(Matrix2f src, Matrix2f dest) {
        if (dest == null) dest = new Matrix2f();
        dest.m00 = src.m00;
        dest.m01 = src.m01;
        dest.m10 = src.m10;
        dest.m11 = src.m11;
        return dest;
    }

    @Override
    public Matrix load(FloatBuffer buf) {
        m00 = buf.get();
        m01 = buf.get();
        m10 = buf.get();
        m11 = buf.get();
        return this;
    }

    @Override
    public Matrix loadTranspose(FloatBuffer buf) {
        m00 = buf.get();
        m10 = buf.get();
        m01 = buf.get();
        m11 = buf.get();
        return this;
    }

    @Override
    public Matrix store(FloatBuffer buf) {
        buf.put(m00);
        buf.put(m01);
        buf.put(m10);
        buf.put(m11);
        return this;
    }

    @Override
    public Matrix storeTranspose(FloatBuffer buf) {
        buf.put(m00);
        buf.put(m10);
        buf.put(m01);
        buf.put(m11);
        return this;
    }

    public static Matrix2f add(Matrix2f left, Matrix2f right, Matrix2f dest) {
        if (dest == null) dest = new Matrix2f();
        dest.m00 = left.m00 + right.m00;
        dest.m01 = left.m01 + right.m01;
        dest.m10 = left.m10 + right.m10;
        dest.m11 = left.m11 + right.m11;
        return dest;
    }

    public static Matrix2f sub(Matrix2f left, Matrix2f right, Matrix2f dest) {
        if (dest == null) dest = new Matrix2f();
        dest.m00 = left.m00 - right.m00;
        dest.m01 = left.m01 - right.m01;
        dest.m10 = left.m10 - right.m10;
        dest.m11 = left.m11 - right.m11;
        return dest;
    }

    public static Matrix2f mul(Matrix2f left, Matrix2f right, Matrix2f dest) {
        if (dest == null) dest = new Matrix2f();
        float m00 = left.m00 * right.m00 + left.m10 * right.m01;
        float m01 = left.m01 * right.m00 + left.m11 * right.m01;
        float m10 = left.m00 * right.m10 + left.m10 * right.m11;
        float m11 = left.m01 * right.m10 + left.m11 * right.m11;
        dest.m00 = m00;
        dest.m01 = m01;
        dest.m10 = m10;
        dest.m11 = m11;
        return dest;
    }

    public static Vector2f transform(Matrix2f left, Vector2f right, Vector2f dest) {
        if (dest == null) dest = new Vector2f();
        float x = left.m00 * right.x + left.m10 * right.y;
        float y = left.m01 * right.x + left.m11 * right.y;
        dest.x = x;
        dest.y = y;
        return dest;
    }

    @Override
    public Matrix transpose() {
        return transpose(this, this);
    }

    public Matrix2f transpose(Matrix2f dest) {
        return transpose(this, dest);
    }

    public static Matrix2f transpose(Matrix2f src, Matrix2f dest) {
        if (dest == null) dest = new Matrix2f();
        float m01 = src.m10;
        float m10 = src.m01;
        dest.m00 = src.m00;
        dest.m01 = m01;
        dest.m10 = m10;
        dest.m11 = src.m11;
        return dest;
    }

    @Override
    public Matrix invert() {
        return invert(this, this);
    }

    public static Matrix2f invert(Matrix2f src, Matrix2f dest) {
        float determinant = src.determinant();
        if (determinant == 0) return null;
        if (dest == null) dest = new Matrix2f();
        float inv = 1f / determinant;
        float t00 = src.m11 * inv;
        float t01 = -src.m01 * inv;
        float t11 = src.m00 * inv;
        float t10 = -src.m10 * inv;
        dest.m00 = t00;
        dest.m01 = t01;
        dest.m10 = t10;
        dest.m11 = t11;
        return dest;
    }

    @Override
    public String toString() {
        return m00 + " " + m10 + " \n" + m01 + " " + m11 + " \n";
    }

    @Override
    public Matrix negate() {
        return negate(this);
    }

    public Matrix2f negate(Matrix2f dest) {
        return negate(this, dest);
    }

    public static Matrix2f negate(Matrix2f src, Matrix2f dest) {
        if (dest == null) dest = new Matrix2f();
        dest.m00 = -src.m00;
        dest.m01 = -src.m01;
        dest.m10 = -src.m10;
        dest.m11 = -src.m11;
        return dest;
    }

    @Override
    public Matrix setIdentity() {
        return setIdentity(this);
    }

    public static Matrix2f setIdentity(Matrix2f src) {
        src.m00 = 1.0f;
        src.m01 = 0.0f;
        src.m10 = 0.0f;
        src.m11 = 1.0f;
        return src;
    }

    @Override
    public Matrix setZero() {
        return setZero(this);
    }

    public static Matrix2f setZero(Matrix2f src) {
        src.m00 = 0.0f;
        src.m01 = 0.0f;
        src.m10 = 0.0f;
        src.m11 = 0.0f;
        return src;
    }

    @Override
    public float determinant() {
        return m00 * m11 - m01 * m10;
    }
}
