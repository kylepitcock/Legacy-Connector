package org.kylepitcock.legacyconnector.vb1_7_3;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import legacyconnector.vb1_7_3.net.minecraft.client.render.Tessellator;

/**
 * The raw OpenGL a Beta mod calls itself, on top of the layer's drawing sinks: textures the mod allocates and fills
 * ({@code glGenTextures}, {@code glTexImage2D}, {@code glTexSubImage2D}) are kept as pixel arrays and handed to the GUI sink as images
 * when bound; {@code glBegin} .. {@code glEnd} vertices are replayed through the {@link Tessellator}, which draws them into whatever
 * the layer is drawing (a GUI, a model context, a captured display list). Triangles, fans, polygons and quad strips become quads.
 */
public final class RawGlB173 {
    private RawGlB173() {}

    // ------------------------------------------------------------------------------------------------------------- textures
    /** Names the mod gets are far from the sink's own small texture ids, so a bind tells the two apart. */
    private static final int FIRST_NAME = 0x40000000;
    private static final AtomicInteger NEXT = new AtomicInteger(FIRST_NAME);
    private static final Map<Integer, Raw> TEXTURES = new ConcurrentHashMap<>();
    private static volatile int bound;

    private static final class Raw {
        int width, height;
        int[] argb = new int[0];
        int sinkId = -1;
        boolean dirty = true;
    }

    public static int genTexture() {
        int name = NEXT.getAndIncrement();
        TEXTURES.put(name, new Raw());
        return name;
    }

    public static void deleteTexture(int name) {
        if (TEXTURES.remove(name) != null && bound == name) bound = 0;
    }

    /** Whether the name is one this class allocated; the bind is then handled here. */
    public static boolean bind(int name) {
        Raw raw = TEXTURES.get(name);
        bound = raw == null ? 0 : name;
        if (raw == null) return false;
        if (raw.width > 0 && raw.height > 0 && !RendererContextB173.active()) {
            if (raw.sinkId < 0) {
                raw.sinkId = GuiDrawB173.load(image(raw));
                raw.dirty = false;
            } else if (raw.dirty) {
                GuiDrawB173.update(raw.sinkId, image(raw));
                raw.dirty = false;
            }
            GuiDrawB173.bind(raw.sinkId);
        }
        return true;
    }

    private static BufferedImage image(Raw raw) {
        BufferedImage image = new BufferedImage(raw.width, raw.height, BufferedImage.TYPE_INT_ARGB);
        image.setRGB(0, 0, raw.width, raw.height, raw.argb, 0, raw.width);
        return image;
    }

    private static int channels(int format) {
        return switch (format) {
            case 6407, 32992 -> 3;       // GL_RGB, GL_BGR
            case 6409, 6406 -> 1;        // GL_LUMINANCE, GL_ALPHA
            case 6410 -> 2;              // GL_LUMINANCE_ALPHA
            default -> 4;                // GL_RGBA, GL_BGRA
        };
    }

    /** One pixel from a byte buffer at {@code at}, in the layout of {@code format}, as ARGB. */
    private static int pixel(ByteBuffer data, int at, int format) {
        switch (format) {
            case 6407: return 0xFF000000 | (data.get(at) & 255) << 16 | (data.get(at + 1) & 255) << 8 | (data.get(at + 2) & 255);
            case 32992: return 0xFF000000 | (data.get(at + 2) & 255) << 16 | (data.get(at + 1) & 255) << 8 | (data.get(at) & 255);
            case 6409: { int l = data.get(at) & 255; return 0xFF000000 | l << 16 | l << 8 | l; }
            case 6406: return (data.get(at) & 255) << 24 | 0xFFFFFF;
            case 6410: { int l = data.get(at) & 255; return (data.get(at + 1) & 255) << 24 | l << 16 | l << 8 | l; }
            case 32993: return (data.get(at + 3) & 255) << 24 | (data.get(at + 2) & 255) << 16 | (data.get(at + 1) & 255) << 8 | (data.get(at) & 255);
            default: return (data.get(at + 3) & 255) << 24 | (data.get(at) & 255) << 16 | (data.get(at + 1) & 255) << 8 | (data.get(at + 2) & 255);
        }
    }

    /** One pixel from an int buffer: BGRA (reversed packing) is already ARGB, RGBA packs the red channel on top. */
    private static int pixel(int value, int format) {
        if (format == 32993) return value;
        return (value & 255) << 24 | (value >>> 8);
    }

    private static void store(Raw raw, int x0, int y0, int w, int h, int format, ByteBuffer bytes, IntBuffer ints) {
        int step = channels(format);
        int bp = bytes == null ? 0 : bytes.position(), ip = ints == null ? 0 : ints.position();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int dx = x0 + x, dy = y0 + y;
                if (dx < 0 || dy < 0 || dx >= raw.width || dy >= raw.height) continue;
                int i = y * w + x;
                int argb;
                if (bytes != null) { int at = bp + i * step; if (at + step > bytes.limit()) continue; argb = pixel(bytes, at, format); }
                else if (ints != null) { if (ip + i >= ints.limit()) continue; argb = pixel(ints.get(ip + i), format); }
                else argb = 0;
                raw.argb[dy * raw.width + dx] = argb;
            }
        }
        raw.dirty = true;
    }

    /** glTexImage2D on the bound texture: level 0 defines its size and pixels (smaller levels are mipmaps the layer never samples). */
    public static void texImage(int level, int width, int height, int format, ByteBuffer bytes, IntBuffer ints) {
        Raw raw = TEXTURES.get(bound);
        if (raw == null || level != 0 || width <= 0 || height <= 0) return;
        raw.width = width;
        raw.height = height;
        raw.argb = new int[width * height];
        if (bytes != null || ints != null) store(raw, 0, 0, width, height, format, bytes, ints);
        raw.dirty = true;
    }

    public static void texSubImage(int level, int x, int y, int width, int height, int format, ByteBuffer bytes, IntBuffer ints) {
        Raw raw = TEXTURES.get(bound);
        if (raw == null || level != 0 || raw.width == 0) return;
        store(raw, x, y, width, height, format, bytes, ints);
    }

    /** glGetTexLevelParameteri for the width (4096) and height (4097) of the bound texture's level 0. */
    public static int levelParameter(int level, int pname) {
        Raw raw = TEXTURES.get(bound);
        if (raw == null || level != 0) return 0;
        return pname == 4096 ? raw.width : pname == 4097 ? raw.height : 0;
    }

    // -------------------------------------------------------------------------------------------------------- immediate mode
    private record V(double x, double y, double z, double u, double v, float r, float g, float b, float a) {}

    private static final class Batch {
        int mode = -1;
        final List<V> vertices = new ArrayList<>();
        double u, v;
        float r = 1, g = 1, b = 1, a = 1;
    }

    private static final ThreadLocal<Batch> BATCH = ThreadLocal.withInitial(Batch::new);

    /** The colour glColor last set, kept here because it is also the colour of the next vertices. */
    public static void color(float r, float g, float b, float a) {
        Batch batch = BATCH.get();
        batch.r = r;
        batch.g = g;
        batch.b = b;
        batch.a = a;
    }

    public static void begin(int mode) {
        Batch batch = BATCH.get();
        batch.mode = mode;
        batch.vertices.clear();
    }

    public static void texCoord(double u, double v) {
        Batch batch = BATCH.get();
        batch.u = u;
        batch.v = v;
    }

    public static void vertex(double x, double y, double z) {
        Batch batch = BATCH.get();
        if (batch.mode >= 0) batch.vertices.add(new V(x, y, z, batch.u, batch.v, batch.r, batch.g, batch.b, batch.a));
    }

    private static void emit(Tessellator t, V v) {
        t.color(v.r, v.g, v.b, v.a);
        t.vertex(v.x, v.y, v.z, v.u, v.v);
    }

    private static void quad(Tessellator t, V a, V b, V c, V d) {
        t.start(7);
        emit(t, a);
        emit(t, b);
        emit(t, c);
        emit(t, d);
        t.draw();
    }

    public static void end() {
        Batch batch = BATCH.get();
        int mode = batch.mode;
        List<V> vs = new ArrayList<>(batch.vertices);
        batch.mode = -1;
        batch.vertices.clear();
        Tessellator t = Tessellator.INSTANCE;
        int n = vs.size();
        switch (mode) {
            case 7 -> { for (int i = 0; i + 3 < n; i += 4) quad(t, vs.get(i), vs.get(i + 1), vs.get(i + 2), vs.get(i + 3)); }
            case 4 -> { for (int i = 0; i + 2 < n; i += 3) quad(t, vs.get(i), vs.get(i + 1), vs.get(i + 2), vs.get(i + 2)); }
            case 5 -> { for (int i = 2; i < n; i++) {
                V a = vs.get(i % 2 == 0 ? i - 2 : i - 1), b = vs.get(i % 2 == 0 ? i - 1 : i - 2);
                quad(t, a, b, vs.get(i), vs.get(i)); } }
            case 6, 9 -> { for (int i = 1; i + 1 < n; i++) quad(t, vs.get(0), vs.get(i), vs.get(i + 1), vs.get(i + 1)); }
            case 8 -> { for (int i = 0; i + 3 < n; i += 2) quad(t, vs.get(i), vs.get(i + 1), vs.get(i + 3), vs.get(i + 2)); }
            case 1, 2, 3 -> {
                if (n < 2) return;
                t.start(mode);
                for (V v : vs) emit(t, v);
                t.draw();
            }
            default -> { }
        }
    }
}
