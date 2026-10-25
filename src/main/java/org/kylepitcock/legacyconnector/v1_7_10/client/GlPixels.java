package org.kylepitcock.legacyconnector.v1_7_10.client;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;

/**
 * Pixel data for texture uploads. LWJGL 2 checked the buffer it was given; LWJGL 3 hands its native address straight to the driver, so
 * a heap buffer (address 0) or one shorter than width x height makes the driver read invalid memory and kills the whole JVM (JourneyMap's
 * minimap texture on an AMD driver). Anything that is not a direct buffer of the needed size is copied into one that is, zero padded.
 */
public final class GlPixels {
    private GlPixels() {}

    /** Bytes the driver reads for an upload of the given size (rows are padded to 4 bytes, GL's default unpack alignment). */
    public static int bytesNeeded(int width, int height, int format, int type) {
        if (width <= 0 || height <= 0) return 0;
        int perPixel;
        switch (type) {
            case 0x8032, 0x8362 -> perPixel = 1; // UNSIGNED_BYTE_3_3_2 and reverse
            case 0x8033, 0x8034, 0x8363, 0x8364, 0x8365, 0x8366 -> perPixel = 2; // 4_4_4_4, 5_5_5_1, 5_6_5 and reverses
            case 0x8035, 0x8036, 0x8367, 0x8368, 0x84FA -> perPixel = 4; // 8_8_8_8, 10_10_10_2, 24_8
            default -> {
                int components = switch (format) {
                    case 0x1908, 0x80E1 -> 4; // RGBA, BGRA
                    case 0x1907, 0x80E0 -> 3; // RGB, BGR
                    case 0x190A, 0x8227 -> 2; // LUMINANCE_ALPHA, RG
                    default -> 1;
                };
                int size = switch (type) {
                    case 0x1403, 0x1402, 0x140B -> 2; // UNSIGNED_SHORT, SHORT, HALF_FLOAT
                    case 0x1405, 0x1404, 0x1406 -> 4; // UNSIGNED_INT, INT, FLOAT
                    default -> 1;
                };
                perPixel = components * size;
            }
        }
        long row = ((long) width * perPixel + 3) & ~3L;
        long total = row * height;
        return total > Integer.MAX_VALUE ? 0 : (int) total;
    }

    /**
     * Uploads copy through one growing direct buffer per thread. A fresh direct buffer per call (mods upload textures every
     * frame: JourneyMap's minimap) is native memory freed only when the heap is collected; with a big heap that is rare, and
     * native memory grew until the game swapped. GL reads client memory before the call returns, so reuse is safe.
     */
    private static final ThreadLocal<ByteBuffer> SCRATCH = new ThreadLocal<>();

    public static ByteBuffer scratch(int bytes) {
        ByteBuffer buf = SCRATCH.get();
        if (buf == null || buf.capacity() < bytes) {
            buf = ByteBuffer.allocateDirect(Math.max(bytes, buf == null ? 1 << 16 : buf.capacity() * 2)).order(ByteOrder.nativeOrder());
            SCRATCH.set(buf);
        }
        buf.clear();
        buf.limit(bytes);
        return buf;
    }

    public static ByteBuffer safe(ByteBuffer pixels, int width, int height, int format, int type) {
        if (pixels == null) return null;
        int need = bytesNeeded(width, height, format, type);
        if (pixels.isDirect() && pixels.remaining() >= need) return pixels;
        ByteBuffer copy = scratch(Math.max(need, pixels.remaining()));
        copy.put(pixels.duplicate());
        copy.clear();
        return copy;
    }

    public static ByteBuffer safe(IntBuffer pixels, int width, int height, int format, int type) {
        if (pixels == null) return null;
        int need = bytesNeeded(width, height, format, type);
        ByteBuffer copy = scratch(Math.max(need, pixels.remaining() * 4));
        copy.asIntBuffer().put(pixels.duplicate());
        copy.clear();
        return copy;
    }

    /** {@code pixels} in the reused scratch buffer, as an int view ready for a GL upload. */
    public static IntBuffer ints(int[] pixels) {
        IntBuffer view = scratch(pixels.length * 4).asIntBuffer();
        view.put(pixels).flip();
        return view;
    }
}
