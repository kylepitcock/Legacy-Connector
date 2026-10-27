package legacyconnector.v1_12_2.org.lwjgl.util.vector;

import java.nio.FloatBuffer;

/** LWJGL 2's quaternion (x, y, z imaginary, w real), used by mods for smooth model and camera rotations. */
public class Quaternion extends Vector implements ReadableVector4f {
    private static final long serialVersionUID = 1L;
    public float x, y, z, w;

    public Quaternion() {
        setIdentity();
    }

    public Quaternion(ReadableVector4f src) {
        set(src);
    }

    public Quaternion(float x, float y, float z, float w) {
        set(x, y, z, w);
    }

    public void set(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public void set(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public void set(float x, float y, float z, float w) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;
    }

    public Quaternion set(ReadableVector4f src) {
        x = src.getX();
        y = src.getY();
        z = src.getZ();
        w = src.getW();
        return this;
    }

    public Quaternion setIdentity() {
        return setIdentity(this);
    }

    public static Quaternion setIdentity(Quaternion q) {
        q.x = 0.0F;
        q.y = 0.0F;
        q.z = 0.0F;
        q.w = 1.0F;
        return q;
    }

    @Override
    public float lengthSquared() {
        return x * x + y * y + z * z + w * w;
    }

    public static Quaternion normalise(Quaternion src, Quaternion dest) {
        float inv = 1.0F / src.length();
        if (dest == null) dest = new Quaternion();
        dest.set(src.x * inv, src.y * inv, src.z * inv, src.w * inv);
        return dest;
    }

    public Quaternion normalise(Quaternion dest) {
        return normalise(this, dest);
    }

    public static float dot(Quaternion left, Quaternion right) {
        return left.x * right.x + left.y * right.y + left.z * right.z + left.w * right.w;
    }

    public Quaternion negate(Quaternion dest) {
        return negate(this, dest);
    }

    /** The conjugate (the inverse rotation of a unit quaternion). */
    public static Quaternion negate(Quaternion src, Quaternion dest) {
        if (dest == null) dest = new Quaternion();
        dest.x = -src.x;
        dest.y = -src.y;
        dest.z = -src.z;
        dest.w = src.w;
        return dest;
    }

    @Override
    public Vector negate() {
        return negate(this, this);
    }

    @Override
    public Vector load(FloatBuffer buf) {
        x = buf.get();
        y = buf.get();
        z = buf.get();
        w = buf.get();
        return this;
    }

    @Override
    public Vector scale(float scale) {
        return scale(scale, this, this);
    }

    public static Quaternion scale(float scale, Quaternion src, Quaternion dest) {
        if (dest == null) dest = new Quaternion();
        dest.x = src.x * scale;
        dest.y = src.y * scale;
        dest.z = src.z * scale;
        dest.w = src.w * scale;
        return dest;
    }

    @Override
    public Quaternion store(FloatBuffer buf) {
        buf.put(x);
        buf.put(y);
        buf.put(z);
        buf.put(w);
        return this;
    }

    @Override
    public float getX() {
        return x;
    }

    @Override
    public float getY() {
        return y;
    }

    @Override
    public float getZ() {
        return z;
    }

    @Override
    public float getW() {
        return w;
    }

    public void setX(float x) {
        this.x = x;
    }

    public void setY(float y) {
        this.y = y;
    }

    public void setZ(float z) {
        this.z = z;
    }

    public void setW(float w) {
        this.w = w;
    }

    @Override
    public String toString() {
        return "Quaternion: " + x + " " + y + " " + z + " " + w;
    }

    public static Quaternion mul(Quaternion left, Quaternion right, Quaternion dest) {
        if (dest == null) dest = new Quaternion();
        dest.set(left.x * right.w + left.w * right.x + left.y * right.z - left.z * right.y,
                left.y * right.w + left.w * right.y + left.z * right.x - left.x * right.z,
                left.z * right.w + left.w * right.z + left.x * right.y - left.y * right.x,
                left.w * right.w - left.x * right.x - left.y * right.y - left.z * right.z);
        return dest;
    }

    /** left * conjugate(right) / |right|^2. */
    public static Quaternion mulInverse(Quaternion left, Quaternion right, Quaternion dest) {
        float n = right.lengthSquared();
        n = n == 0.0F ? n : 1.0F / n;
        if (dest == null) dest = new Quaternion();
        dest.set((left.x * right.w - left.w * right.x - left.y * right.z + left.z * right.y) * n,
                (left.y * right.w - left.w * right.y - left.z * right.x + left.x * right.z) * n,
                (left.z * right.w - left.w * right.z - left.x * right.y + left.y * right.x) * n,
                (left.w * right.w + left.x * right.x + left.y * right.y + left.z * right.z) * n);
        return dest;
    }

    /** The rotation by {@code a1.w} radians about the axis (a1.x, a1.y, a1.z). */
    public final void setFromAxisAngle(Vector4f a1) {
        x = a1.x;
        y = a1.y;
        z = a1.z;
        float n = (float) Math.sqrt(x * x + y * y + z * z);
        float s = (float) (Math.sin(0.5D * a1.w) / n);
        x *= s;
        y *= s;
        z *= s;
        w = (float) Math.cos(0.5D * a1.w);
    }

    public final Quaternion setFromMatrix(Matrix4f m) {
        return setFromMatrix(m, this);
    }

    public static Quaternion setFromMatrix(Matrix4f m, Quaternion q) {
        return q.setFromMat(m.m00, m.m01, m.m02, m.m10, m.m11, m.m12, m.m20, m.m21, m.m22);
    }

    private Quaternion setFromMat(float m00, float m01, float m02, float m10, float m11, float m12, float m20, float m21, float m22) {
        float tr = m00 + m11 + m22;
        if (tr >= 0.0D) {
            float s = (float) Math.sqrt(tr + 1.0D);
            w = s * 0.5F;
            s = 0.5F / s;
            x = (m21 - m12) * s;
            y = (m02 - m20) * s;
            z = (m10 - m01) * s;
        } else {
            float max = Math.max(Math.max(m00, m11), m22);
            if (max == m00) {
                float s = (float) Math.sqrt(m00 - (m11 + m22) + 1.0D);
                x = s * 0.5F;
                s = 0.5F / s;
                y = (m01 + m10) * s;
                z = (m20 + m02) * s;
                w = (m21 - m12) * s;
            } else if (max == m11) {
                float s = (float) Math.sqrt(m11 - (m22 + m00) + 1.0D);
                y = s * 0.5F;
                s = 0.5F / s;
                z = (m12 + m21) * s;
                x = (m01 + m10) * s;
                w = (m02 - m20) * s;
            } else {
                float s = (float) Math.sqrt(m22 - (m00 + m11) + 1.0D);
                z = s * 0.5F;
                s = 0.5F / s;
                x = (m20 + m02) * s;
                y = (m12 + m21) * s;
                w = (m10 - m01) * s;
            }
        }
        return this;
    }
}
