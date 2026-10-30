package legacyconnector.v1_12_2.org.lwjgl.opengl;

import java.nio.ByteBuffer;
import org.kylepitcock.legacyconnector.v1_7_10.client.ClientArrays;

/**
 * Legacy {@code org.lwjgl.opengl.GL15} for 1.12.2 mods: the 1.7.10 shim's buffer objects (emulated for fixed-function arrays).
 * Mapping, partial updates and queries of an emulated array/element buffer work on its memory copy; everything else
 * passes through to LWJGL 3.
 */
public class GL15 extends legacyconnector.v1_7_10.org.lwjgl.opengl.GL15 {
    private static final int GL_BUFFER_SIZE = 0x8764, GL_BUFFER_USAGE = 0x8765, GL_BUFFER_ACCESS = 0x88BB, GL_BUFFER_MAPPED = 0x88BC;
    private static final int GL_STATIC_DRAW = 0x88E4, GL_READ_WRITE = 0x88BA;

    protected GL15() {
    }

    public static ByteBuffer glMapBuffer(int target, int access, ByteBuffer oldBuffer) {
        ByteBuffer data = ClientArrays.boundData(target);
        if (data != null) return data.duplicate().clear().order(java.nio.ByteOrder.nativeOrder());
        return org.lwjgl.opengl.GL15.glMapBuffer(target, access, oldBuffer);
    }

    public static ByteBuffer glMapBuffer(int target, int access, long length, ByteBuffer oldBuffer) {
        ByteBuffer data = ClientArrays.boundData(target);
        if (data != null) {
            ByteBuffer view = data.duplicate().clear();
            view.limit((int) Math.min(view.capacity(), length));
            return view.slice().order(java.nio.ByteOrder.nativeOrder());
        }
        return org.lwjgl.opengl.GL15.glMapBuffer(target, access, length, oldBuffer);
    }

    public static boolean glUnmapBuffer(int target) {
        if (ClientArrays.boundData(target) != null) return true; // the mapped view wrote straight into the copy
        return org.lwjgl.opengl.GL15.glUnmapBuffer(target);
    }

    public static void glBufferSubData(int target, long offset, ByteBuffer data) {
        ByteBuffer dest = ClientArrays.boundData(target);
        if (dest == null) {
            org.lwjgl.opengl.GL15.glBufferSubData(target, offset, data);
            return;
        }
        int n = Math.min(data.remaining(), dest.capacity() - (int) offset);
        for (int i = 0; i < n; i++) dest.put((int) offset + i, data.get(data.position() + i));
    }

    public static void glGetBufferSubData(int target, long offset, ByteBuffer data) {
        ByteBuffer src = ClientArrays.boundData(target);
        if (src == null) {
            org.lwjgl.opengl.GL15.glGetBufferSubData(target, offset, data);
            return;
        }
        int n = Math.min(data.remaining(), src.capacity() - (int) offset);
        for (int i = 0; i < n; i++) data.put(data.position() + i, src.get((int) offset + i));
    }

    public static int glGetBufferParameteri(int target, int pname) {
        ByteBuffer data = ClientArrays.boundData(target);
        if (data == null) return org.lwjgl.opengl.GL15.glGetBufferParameteri(target, pname);
        return switch (pname) {
            case GL_BUFFER_SIZE -> data.capacity();
            case GL_BUFFER_USAGE -> GL_STATIC_DRAW;
            case GL_BUFFER_ACCESS -> GL_READ_WRITE;
            case GL_BUFFER_MAPPED -> 0;
            default -> 0;
        };
    }

    public static boolean glIsBuffer(int buffer) {
        if (ClientArrays.isEmulated(buffer)) return ClientArrays.exists(buffer);
        return org.lwjgl.opengl.GL15.glIsBuffer(buffer);
    }

    public static int glGenQueries() {
        return org.lwjgl.opengl.GL15.glGenQueries();
    }

    public static void glDeleteQueries(int id) {
        org.lwjgl.opengl.GL15.glDeleteQueries(id);
    }
}
