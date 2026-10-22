package org.kylepitcock.legacyconnector.v1_7_10.client;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The fixed-function vertex arrays and buffer objects of LWJGL 2 ({@code glVertexPointer}, {@code glDrawArrays} on
 * a bound VBO), which 1.21's core profile no longer has. Buffer objects are kept in memory under ids of their own,
 * and draw calls feed the immediate-mode emulation (so mods' VBO model loaders keep rendering). Only the legacy
 * calls ever see these ids; nothing here touches the GPU.
 */
public final class ClientArrays {
    public static final int GL_ARRAY_BUFFER = 0x8892;
    public static final int VERTEX_ARRAY = 0x8074, NORMAL_ARRAY = 0x8075, COLOR_ARRAY = 0x8076, TEXTURE_COORD_ARRAY = 0x8078;
    private static final int GL_FLOAT = 0x1406, GL_UNSIGNED_BYTE = 0x1401, GL_INT = 0x1404, GL_SHORT = 0x1402;

    private static final AtomicInteger NEXT = new AtomicInteger(0x40000000);
    private static final ConcurrentHashMap<Integer, ByteBuffer> BUFFERS = new ConcurrentHashMap<>();

    /** Per-thread state: GL state is per context, and legacy code uses one context. */
    private static int boundArray;
    private static final Pointer VERTEX = new Pointer(), NORMAL = new Pointer(), TEXCOORD = new Pointer(), COLOR = new Pointer();

    private static final class Pointer {
        boolean enabled;
        int size, type, stride;
        long offset;
        ByteBuffer client; // pointer into client memory (a Buffer passed in directly) instead of a VBO
        int buffer; // the VBO it was set against
    }

    private ClientArrays() {}

    public static boolean isEmulated(int id) {
        return id >= 0x40000000;
    }

    public static int genBuffer() {
        int id = NEXT.getAndIncrement();
        BUFFERS.put(id, ByteBuffer.allocate(0));
        return id;
    }

    public static void deleteBuffer(int id) {
        BUFFERS.remove(id);
        if (boundArray == id) boundArray = 0;
    }

    public static void bind(int target, int id) {
        if (target == GL_ARRAY_BUFFER) boundArray = id;
    }

    public static boolean data(int target, int id, Buffer data) {
        if (!isEmulated(id) && !isEmulated(target == GL_ARRAY_BUFFER ? boundArray : 0)) return false;
        int buffer = target == GL_ARRAY_BUFFER ? boundArray : id;
        if (!isEmulated(buffer)) return false;
        BUFFERS.put(buffer, copy(data));
        return true;
    }

    public static boolean data(int target, Buffer data) {
        return data(target, boundArray, data);
    }

    public static boolean dataSize(int target, long size) {
        int buffer = target == GL_ARRAY_BUFFER ? boundArray : 0;
        if (!isEmulated(buffer)) return false;
        BUFFERS.put(buffer, ByteBuffer.allocate((int) size).order(ByteOrder.nativeOrder()));
        return true;
    }

    private static ByteBuffer copy(Buffer data) {
        ByteBuffer out;
        if (data instanceof FloatBuffer f) {
            out = ByteBuffer.allocate(f.remaining() * 4).order(ByteOrder.nativeOrder());
            out.asFloatBuffer().put(f.duplicate());
        } else if (data instanceof IntBuffer i) {
            out = ByteBuffer.allocate(i.remaining() * 4).order(ByteOrder.nativeOrder());
            out.asIntBuffer().put(i.duplicate());
        } else if (data instanceof ByteBuffer b) {
            out = ByteBuffer.allocate(b.remaining()).order(ByteOrder.nativeOrder());
            out.put(b.duplicate());
            out.flip();
            return out;
        } else {
            out = ByteBuffer.allocate(0);
        }
        return out;
    }

    public static void enable(int array, boolean on) {
        Pointer p = pointer(array);
        if (p != null) p.enabled = on;
    }

    private static Pointer pointer(int array) {
        return switch (array) {
            case VERTEX_ARRAY -> VERTEX;
            case NORMAL_ARRAY -> NORMAL;
            case TEXTURE_COORD_ARRAY -> TEXCOORD;
            case COLOR_ARRAY -> COLOR;
            default -> null;
        };
    }

    public static void setPointer(int array, int size, int type, int stride, long offset) {
        Pointer p = pointer(array);
        if (p == null) return;
        p.size = size;
        p.type = type;
        p.stride = stride;
        p.offset = offset;
        p.client = null;
        p.buffer = boundArray;
    }

    public static void setPointer(int array, int size, int type, int stride, Buffer data) {
        Pointer p = pointer(array);
        if (p == null) return;
        p.size = size;
        p.type = type;
        p.stride = stride;
        p.offset = 0;
        p.client = copy(data);
        p.buffer = 0;
    }

    private static float read(Pointer p, ByteBuffer src, int index, int component) {
        int width = switch (p.type) {
            case GL_FLOAT, GL_INT -> 4;
            case GL_SHORT -> 2;
            default -> 1;
        };
        int stride = p.stride != 0 ? p.stride : p.size * width;
        int at = (int) p.offset + index * stride + component * width;
        if (at < 0 || at + width > src.limit()) return 0;
        return switch (p.type) {
            case GL_FLOAT -> src.getFloat(at);
            case GL_INT -> src.getInt(at);
            case GL_SHORT -> src.getShort(at);
            case GL_UNSIGNED_BYTE -> (src.get(at) & 255) / 255.0F;
            default -> src.get(at);
        };
    }

    private static ByteBuffer source(Pointer p) {
        return p.client != null ? p.client : BUFFERS.get(p.buffer);
    }

    /** Draws arrays through the immediate-mode emulation; returns false when the arrays are not emulated ones. */
    public static boolean draw(int mode, int first, int count, Emitter out) {
        if (!VERTEX.enabled) return true;
        ByteBuffer vs = source(VERTEX);
        if (vs == null) return false;
        ByteBuffer ns = NORMAL.enabled ? source(NORMAL) : null;
        ByteBuffer ts = TEXCOORD.enabled ? source(TEXCOORD) : null;
        out.begin(mode);
        for (int i = first; i < first + count; i++) {
            if (ts != null) out.texCoord(read(TEXCOORD, ts, i, 0), read(TEXCOORD, ts, i, 1));
            if (ns != null) out.normal(read(NORMAL, ns, i, 0), read(NORMAL, ns, i, 1), read(NORMAL, ns, i, 2));
            out.vertex(read(VERTEX, vs, i, 0), read(VERTEX, vs, i, 1), VERTEX.size > 2 ? read(VERTEX, vs, i, 2) : 0);
        }
        out.end();
        return true;
    }

    /** Receives the vertices (the GL11 shim's immediate mode). */
    public interface Emitter {
        void begin(int mode);

        void texCoord(float u, float v);

        void normal(float x, float y, float z);

        void vertex(float x, float y, float z);

        void end();
    }
}
