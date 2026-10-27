package legacyconnector.v1_12_2.org.lwjgl.util.vector;

import java.nio.FloatBuffer;

public interface ReadableVector {
    float length();

    float lengthSquared();

    ReadableVector store(FloatBuffer buf);
}
