package legacyconnector.v1_12_2.org.lwjgl.util.vector;

import java.nio.FloatBuffer;

public class Vector3f extends Vector implements ReadableVector3f, WritableVector3f {
    public float x;
    public float y;
    public float z;

    public Vector3f() {
    }

    public Vector3f(ReadableVector3f src) {
        set(src);
    }

    public Vector3f(float x, float y, float z) {
        set(x, y, z);
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

    public Vector3f set(ReadableVector3f src) {
        x = src.getX();
        y = src.getY();
        z = src.getZ();
        return this;
    }

    @Override
    public float lengthSquared() {
        return x * x + y * y + z * z;
    }

    public Vector3f translate(float x, float y, float z) {
        this.x += x;
        this.y += y;
        this.z += z;
        return this;
    }

    public static Vector3f add(Vector3f left, Vector3f right, Vector3f dest) {
        if (dest == null) return new Vector3f(left.x + right.x, left.y + right.y, left.z + right.z);
        dest.set(left.x + right.x, left.y + right.y, left.z + right.z);
        return dest;
    }

    public static Vector3f sub(Vector3f left, Vector3f right, Vector3f dest) {
        if (dest == null) return new Vector3f(left.x - right.x, left.y - right.y, left.z - right.z);
        dest.set(left.x - right.x, left.y - right.y, left.z - right.z);
        return dest;
    }

    public static Vector3f cross(Vector3f left, Vector3f right, Vector3f dest) {
        if (dest == null) dest = new Vector3f();
        dest.set(left.y * right.z - left.z * right.y, right.x * left.z - right.z * left.x, left.x * right.y - left.y * right.x);
        return dest;
    }

    @Override
    public Vector negate() {
        x = -x;
        y = -y;
        z = -z;
        return this;
    }

    public Vector3f negate(Vector3f dest) {
        if (dest == null) dest = new Vector3f();
        dest.x = -x;
        dest.y = -y;
        dest.z = -z;
        return dest;
    }

    public Vector3f normalise(Vector3f dest) {
        float l = length();
        if (dest == null) dest = new Vector3f(x / l, y / l, z / l);
        else dest.set(x / l, y / l, z / l);
        return dest;
    }

    public static float dot(Vector3f left, Vector3f right) {
        return left.x * right.x + left.y * right.y + left.z * right.z;
    }

    public static float angle(Vector3f a, Vector3f b) {
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
        return this;
    }

    @Override
    public Vector scale(float scale) {
        x *= scale;
        y *= scale;
        z *= scale;
        return this;
    }

    @Override
    public Vector store(FloatBuffer buf) {
        buf.put(x);
        buf.put(y);
        buf.put(z);
        return this;
    }

    @Override
    public String toString() {
        return "Vector3f[" + x + ", " + y + ", " + z + "]";
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
    public float getZ() {
        return z;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Vector3f v && x == v.x && y == v.y && z == v.z;
    }

    @Override
    public int hashCode() {
        return (Float.hashCode(x) * 31 + Float.hashCode(y)) * 31 + Float.hashCode(z);
    }
}
