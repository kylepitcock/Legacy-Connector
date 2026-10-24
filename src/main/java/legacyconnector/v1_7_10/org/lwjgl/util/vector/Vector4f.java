package legacyconnector.v1_7_10.org.lwjgl.util.vector;

import java.nio.FloatBuffer;

public class Vector4f extends Vector implements ReadableVector4f, WritableVector4f {
    public float x;
    public float y;
    public float z;
    public float w;

    public Vector4f() {
    }

    public Vector4f(ReadableVector4f src) {
        set(src);
    }

    public Vector4f(float x, float y, float z, float w) {
        set(x, y, z, w);
    }

    @Override
    public void set(float x, float y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public void set(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    public void set(float x, float y, float z, float w) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;
    }

    public Vector4f set(ReadableVector4f src) {
        x = src.getX();
        y = src.getY();
        z = src.getZ();
        w = src.getW();
        return this;
    }

    @Override
    public float lengthSquared() {
        return x * x + y * y + z * z + w * w;
    }

    public Vector4f translate(float x, float y, float z, float w) {
        this.x += x;
        this.y += y;
        this.z += z;
        this.w += w;
        return this;
    }

    public static Vector4f add(Vector4f left, Vector4f right, Vector4f dest) {
        if (dest == null) return new Vector4f(left.x + right.x, left.y + right.y, left.z + right.z, left.w + right.w);
        dest.set(left.x + right.x, left.y + right.y, left.z + right.z, left.w + right.w);
        return dest;
    }

    public static Vector4f sub(Vector4f left, Vector4f right, Vector4f dest) {
        if (dest == null) return new Vector4f(left.x - right.x, left.y - right.y, left.z - right.z, left.w - right.w);
        dest.set(left.x - right.x, left.y - right.y, left.z - right.z, left.w - right.w);
        return dest;
    }

    @Override
    public Vector negate() {
        x = -x;
        y = -y;
        z = -z;
        w = -w;
        return this;
    }

    public Vector4f negate(Vector4f dest) {
        if (dest == null) dest = new Vector4f();
        dest.x = -x;
        dest.y = -y;
        dest.z = -z;
        dest.w = -w;
        return dest;
    }

    public Vector4f normalise(Vector4f dest) {
        float l = length();
        if (dest == null) dest = new Vector4f(x / l, y / l, z / l, w / l);
        else dest.set(x / l, y / l, z / l, w / l);
        return dest;
    }

    public static float dot(Vector4f left, Vector4f right) {
        return left.x * right.x + left.y * right.y + left.z * right.z + left.w * right.w;
    }

    public static float angle(Vector4f a, Vector4f b) {
        float dls = dot(a, b) / (a.length() * b.length());
        if (dls < -1.0F) dls = -1.0F;
        else if (dls > 1.0F) dls = 1.0F;
        return (float) Math.acos(dls);
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
        x *= scale;
        y *= scale;
        z *= scale;
        w *= scale;
        return this;
    }

    @Override
    public Vector store(FloatBuffer buf) {
        buf.put(x);
        buf.put(y);
        buf.put(z);
        buf.put(w);
        return this;
    }

    @Override
    public String toString() {
        return "Vector4f: " + x + " " + y + " " + z + " " + w;
    }

    @Override
    public final float getX() {
        return x;
    }

    @Override
    public final float getY() {
        return y;
    }

    @Override
    public final float getZ() {
        return z;
    }

    @Override
    public final float getW() {
        return w;
    }

    @Override
    public final void setX(float x) {
        this.x = x;
    }

    @Override
    public final void setY(float y) {
        this.y = y;
    }

    @Override
    public final void setZ(float z) {
        this.z = z;
    }

    @Override
    public final void setW(float w) {
        this.w = w;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Vector4f v && x == v.x && y == v.y && z == v.z && w == v.w;
    }

    @Override
    public int hashCode() {
        return ((Float.hashCode(x) * 31 + Float.hashCode(y)) * 31 + Float.hashCode(z)) * 31 + Float.hashCode(w);
    }
}
