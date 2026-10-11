package mccompat.v1_7_10.org.lwjgl.opengl;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import org.kylepitcock.mccompat.v1_7_10.client.ClientArrays;

/**
 * Legacy {@code org.lwjgl.opengl.GL15}: buffer objects. Legacy code uses them with fixed-function vertex arrays
 * (see {@link ClientArrays}), so they are kept in memory under ids that never reach the driver; calls on real
 * buffer ids pass through to LWJGL 3.
 */
public class GL15 extends org.lwjgl.opengl.GL15 {
    protected GL15() {
    }

    public static int glGenBuffers() {
        return ClientArrays.genBuffer();
    }

    public static void glGenBuffers(IntBuffer buffers) {
        int p = buffers.position();
        for (int i = buffers.position(); i < buffers.limit(); i++) buffers.put(i, ClientArrays.genBuffer());
        buffers.position(p);
    }

    public static void glDeleteBuffers(int buffer) {
        if (ClientArrays.isEmulated(buffer)) ClientArrays.deleteBuffer(buffer);
        else org.lwjgl.opengl.GL15.glDeleteBuffers(buffer);
    }

    public static void glDeleteBuffers(IntBuffer buffers) {
        for (int i = buffers.position(); i < buffers.limit(); i++) glDeleteBuffers(buffers.get(i));
    }

    public static void glBindBuffer(int target, int buffer) {
        ClientArrays.bind(target, buffer);
        if (buffer != 0 && !ClientArrays.isEmulated(buffer)) org.lwjgl.opengl.GL15.glBindBuffer(target, buffer);
    }

    public static void glBufferData(int target, FloatBuffer data, int usage) {
        if (!ClientArrays.data(target, data)) org.lwjgl.opengl.GL15.glBufferData(target, data, usage);
    }

    public static void glBufferData(int target, IntBuffer data, int usage) {
        if (!ClientArrays.data(target, data)) org.lwjgl.opengl.GL15.glBufferData(target, data, usage);
    }

    public static void glBufferData(int target, ByteBuffer data, int usage) {
        if (!ClientArrays.data(target, data)) org.lwjgl.opengl.GL15.glBufferData(target, data, usage);
    }

    public static void glBufferData(int target, long size, int usage) {
        if (!ClientArrays.dataSize(target, size)) org.lwjgl.opengl.GL15.glBufferData(target, size, usage);
    }
}
