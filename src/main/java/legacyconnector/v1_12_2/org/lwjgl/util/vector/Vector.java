package legacyconnector.v1_12_2.org.lwjgl.util.vector;

import java.io.Serializable;
import java.nio.FloatBuffer;

/** Base of the LWJGL 2 vectors. */
public abstract class Vector implements Serializable, ReadableVector {
    protected Vector() {
    }

    @Override
    public final float length() {
        return (float) Math.sqrt(lengthSquared());
    }

    @Override
    public abstract float lengthSquared();

    public abstract Vector load(FloatBuffer buf);

    public abstract Vector negate();

    public final Vector normalise() {
        float len = length();
        if (len != 0.0F) {
            float l = 1.0F / len;
            return scale(l);
        }
        throw new IllegalStateException("Zero length vector");
    }

    @Override
    public abstract Vector store(FloatBuffer buf);

    public abstract Vector scale(float scale);
}
