package legacyconnector.vb1_7_3.org.lwjgl;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;

/** LWJGL 2's {@code BufferUtils} (native-order direct buffers) without LWJGL, so b1.7.3 client builds load on a dedicated server too. */
public final class BufferUtils {
    private BufferUtils() {}

    public static ByteBuffer createByteBuffer(int size) {
        return ByteBuffer.allocateDirect(size).order(ByteOrder.nativeOrder());
    }

    public static ShortBuffer createShortBuffer(int size) {
        return createByteBuffer(size << 1).asShortBuffer();
    }

    public static CharBuffer createCharBuffer(int size) {
        return createByteBuffer(size << 1).asCharBuffer();
    }

    public static IntBuffer createIntBuffer(int size) {
        return createByteBuffer(size << 2).asIntBuffer();
    }

    public static LongBuffer createLongBuffer(int size) {
        return createByteBuffer(size << 3).asLongBuffer();
    }

    public static FloatBuffer createFloatBuffer(int size) {
        return createByteBuffer(size << 2).asFloatBuffer();
    }

    public static DoubleBuffer createDoubleBuffer(int size) {
        return createByteBuffer(size << 3).asDoubleBuffer();
    }

    public static int getElementSizeExponent(Buffer buffer) {
        if (buffer instanceof ByteBuffer) return 0;
        if (buffer instanceof ShortBuffer || buffer instanceof CharBuffer) return 1;
        if (buffer instanceof FloatBuffer || buffer instanceof IntBuffer) return 2;
        if (buffer instanceof LongBuffer || buffer instanceof DoubleBuffer) return 3;
        throw new IllegalStateException("Unsupported buffer type: " + buffer);
    }

    public static int getOffset(Buffer buffer) {
        return buffer.position() << getElementSizeExponent(buffer);
    }

    public static void zeroBuffer(ByteBuffer b) {
        for (int i = b.position(); i < b.limit(); i++) b.put(i, (byte) 0);
    }
}
