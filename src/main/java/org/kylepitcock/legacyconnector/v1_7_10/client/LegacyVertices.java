package org.kylepitcock.legacyconnector.v1_7_10.client;

import java.util.ArrayList;
import java.util.List;

/**
 * Assembles legacy immediate-mode geometry (Tessellator, glBegin/glEnd) into quads for {@link RenderContext}:
 * quads as they are, triangles/fans/strips as degenerate quads. Lines and points are dropped.
 *
 * <p>An instance belongs to one thread (the Tessellator and the glBegin vertices are per thread). The vertex arrays of a draw
 * that is emitted right away are reused by the next draws, so a renderer drawing thousands of vertices a frame allocates none;
 * a draw kept for later ({@link #take}, display lists and vertex states) keeps its arrays.</p>
 */
public final class LegacyVertices {
    public static final int GL_POINTS = 0, GL_LINES = 1, GL_LINE_LOOP = 2, GL_LINE_STRIP = 3, GL_TRIANGLES = 4,
            GL_TRIANGLE_STRIP = 5, GL_TRIANGLE_FAN = 6, GL_QUADS = 7, GL_QUAD_STRIP = 8, GL_POLYGON = 9;
    /** Doubles per vertex: x, y, z, u, v, argb, light, own colour. */
    public static final int STRIDE = 8;
    /** Vertex arrays kept for reuse at most (a huge draw must not keep its memory for good). */
    private static final int MAX_SPARE = 16384;

    private final List<double[]> vertices = new ArrayList<>();
    private final ArrayList<double[]> spare = new ArrayList<>();
    private int mode = GL_QUADS;
    private boolean drawing;
    private double u, v;
    private int argb = 0xFFFFFFFF;
    private boolean hasColor;
    private int light = -1;
    private float nx, ny = 1, nz;
    private boolean hasNormal;
    private double tx, ty, tz;
    private boolean colorDisabled;

    public void begin(int mode) {
        this.mode = mode;
        this.drawing = true;
        recycle();
        hasColor = false;
        hasNormal = false;
        colorDisabled = false;
        light = -1;
    }

    /** Drops geometry begun but never ended (a legacy renderer that threw midway). */
    public void cancel() {
        drawing = false;
        recycle();
    }

    /** Clears the current vertices, keeping their arrays for the next draws. */
    private void recycle() {
        int n = vertices.size();
        for (int i = 0; i < n && spare.size() < MAX_SPARE; i++) spare.add(vertices.get(i));
        vertices.clear();
    }

    public boolean isDrawing() {
        return drawing;
    }

    public void uv(double u, double v) {
        this.u = u;
        this.v = v;
    }

    public void color(float r, float g, float b, float a) {
        if (colorDisabled) return;
        argb = clamp(a) << 24 | clamp(r) << 16 | clamp(g) << 8 | clamp(b);
        hasColor = true;
    }

    public void color(int r, int g, int b, int a) {
        if (colorDisabled) return;
        argb = (a & 255) << 24 | (r & 255) << 16 | (g & 255) << 8 | (b & 255);
        hasColor = true;
    }

    public void disableColor() {
        colorDisabled = true;
    }

    /** The explicit colour (ARGB) or -1 when none is set, for {@link #setColorState}. */
    public long colorState() {
        return hasColor ? argb & 0xFFFFFFFFL : -1L;
    }

    public void setColorState(long state) {
        hasColor = state >= 0;
        if (hasColor) argb = (int) state;
    }

    private static int clamp(float f) {
        return Math.max(0, Math.min(255, (int) (f * 255.0F)));
    }

    public void light(int packed) {
        this.light = packed;
    }

    public void normal(float x, float y, float z) {
        nx = x;
        ny = y;
        nz = z;
        hasNormal = true;
    }

    public void translation(double x, double y, double z) {
        tx = x;
        ty = y;
        tz = z;
    }

    public void addTranslation(double x, double y, double z) {
        tx += x;
        ty += y;
        tz += z;
    }

    public void vertex(double x, double y, double z) {
        if (!drawing) return;
        RenderContext.State s = hasColor && light >= 0 ? null : RenderContext.state();
        int color = hasColor ? argb : RenderContext.argb(s);
        int n = spare.size();
        double[] a = n > 0 ? spare.remove(n - 1) : new double[STRIDE];
        a[0] = x + tx;
        a[1] = y + ty;
        a[2] = z + tz;
        a[3] = u;
        a[4] = v;
        a[5] = color & 0xFFFFFFFFL;
        a[6] = light >= 0 ? light : RenderContext.vertexLight(s);
        // [7]: the vertex carries its own colour; otherwise it takes the colour current when the geometry is emitted (display lists replay later)
        a[7] = hasColor ? 1.0D : 0.0D;
        vertices.add(a);
    }

    /**
     * The vertices added since {@link #begin} ({x, y, z, u, v, argb, light, own colour}); live list. Its arrays are reused once the
     * draw ends: a caller keeping them copies them, and arrays put in it must belong to this list alone.
     */
    public List<double[]> mcc$vertices() {
        return vertices;
    }

    public double mcc$tx() {
        return tx;
    }

    public double mcc$ty() {
        return ty;
    }

    public double mcc$tz() {
        return tz;
    }

    /** A finished draw: what {@link #emit} needs to produce its quads later (display lists keep these). */
    public record Snapshot(int mode, List<double[]> vertices, float nx, float ny, float nz, boolean hasNormal) {}

    /** Ends the draw without emitting it; the snapshot owns its vertex arrays. */
    public Snapshot take() {
        drawing = false;
        Snapshot s = new Snapshot(mode, new ArrayList<>(vertices), nx, ny, nz, hasNormal);
        vertices.clear();
        return s;
    }

    /** Emits the assembled primitives; returns the vertex count like the legacy Tessellator. */
    public int end() {
        drawing = false;
        int n = vertices.size();
        try {
            emit(new Snapshot(mode, vertices, nx, ny, nz, hasNormal));
        } finally {
            recycle();
        }
        return n;
    }

    public static void emit(Snapshot s) {
        List<double[]> vertices = s.vertices();
        int n = vertices.size();
        RenderContext.State st = RenderContext.state();
        long current = RenderContext.argb(st) & 0xFFFFFFFFL;
        // vertices without a colour of their own take the current one (in place: the arrays belong to this draw)
        for (int i = 0; i < n; i++) {
            double[] v = vertices.get(i);
            if (v.length > 7 && v[7] == 0.0D && (long) v[5] != current) v[5] = current;
        }
        switch (s.mode()) {
            case GL_QUADS -> {
                for (int i = 0; i + 3 < n; i += 4) emit(st, s, vertices.get(i), vertices.get(i + 1), vertices.get(i + 2), vertices.get(i + 3));
            }
            case GL_TRIANGLES -> {
                for (int i = 0; i + 2 < n; i += 3) emit(st, s, vertices.get(i), vertices.get(i + 1), vertices.get(i + 2), vertices.get(i + 2));
            }
            case GL_TRIANGLE_FAN, GL_POLYGON -> {
                for (int i = 1; i + 1 < n; i++) emit(st, s, vertices.get(0), vertices.get(i), vertices.get(i + 1), vertices.get(i + 1));
            }
            case GL_TRIANGLE_STRIP -> {
                for (int i = 0; i + 2 < n; i++) {
                    if ((i & 1) == 0) emit(st, s, vertices.get(i), vertices.get(i + 1), vertices.get(i + 2), vertices.get(i + 2));
                    else emit(st, s, vertices.get(i + 1), vertices.get(i), vertices.get(i + 2), vertices.get(i + 2));
                }
            }
            case GL_QUAD_STRIP -> {
                for (int i = 0; i + 3 < n; i += 2) emit(st, s, vertices.get(i), vertices.get(i + 1), vertices.get(i + 3), vertices.get(i + 2));
            }
            case GL_LINES -> {
                for (int i = 0; i + 1 < n; i += 2) line(st, s, vertices.get(i), vertices.get(i + 1));
            }
            case GL_LINE_STRIP -> {
                for (int i = 0; i + 1 < n; i++) line(st, s, vertices.get(i), vertices.get(i + 1));
            }
            case GL_LINE_LOOP -> {
                for (int i = 0; i < n && n > 1; i++) line(st, s, vertices.get(i), vertices.get((i + 1) % n));
            }
            default -> {
                // points have no counterpart in the entity pipelines
            }
        }
        // 1.7.10 drew immediately: in a screen, what comes later (blitted textures, items) must cover this draw, so it is
        // not left batched until the end of the frame (Thaumonomicon lines showed over the research icons)
        var gui = RenderContext.gui(st);
        if (gui != null) {
            // the vertices already carry glColor (or their own colour, which in 1.7.10 replaced it): the shader colour the
            // screen applies would multiply it in a second time (Thaumcraft's aspects came out dark)
            float[] shader = com.mojang.blaze3d.systems.RenderSystem.getShaderColor();
            float r = shader[0], g = shader[1], b = shader[2], a = shader[3];
            com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            try {
                gui.flush();
            } finally {
                com.mojang.blaze3d.systems.RenderSystem.setShaderColor(r, g, b, a);
            }
        }
    }

    /**
     * A line is a thin quad (the pipelines draw no lines): one GUI unit wide in screens, a hair wide in the world; it lies in the xy plane
     * when the segment does, and otherwise is widened sideways.
     */
    private static void line(RenderContext.State st, Snapshot s, double[] a, double[] b) {
        double dx = b[0] - a[0], dy = b[1] - a[1], dz = b[2] - a[2];
        double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 1.0E-9) return;
        boolean gui = RenderContext.gui(st) != null;
        // in the world a real line, a pixel wide at any distance (a hair-wide quad vanished into dots: CustomOreGen's wireframes)
        if (!gui && RenderContext.line(a, b)) return;
        double half = gui ? 0.5D : 0.01D;
        double px, py, pz;
        if (Math.abs(dz) < 1.0E-6) {
            double l2 = Math.sqrt(dx * dx + dy * dy);
            px = -dy / l2 * half;
            py = dx / l2 * half;
            pz = 0;
        } else {
            // perpendicular to the segment and to the up axis (or the x axis for a vertical one)
            double ux = 0, uy = 1, uz = 0;
            if (Math.abs(dy) > 0.999D * len) {
                ux = 1;
                uy = 0;
            }
            px = dy * uz - dz * uy;
            py = dz * ux - dx * uz;
            pz = dx * uy - dy * ux;
            double l2 = Math.sqrt(px * px + py * py + pz * pz);
            px = px / l2 * half;
            py = py / l2 * half;
            pz = pz / l2 * half;
        }
        double[] v0 = a.clone(), v1 = b.clone(), v2 = b.clone(), v3 = a.clone();
        v0[0] += px; v0[1] += py; v0[2] += pz;
        v1[0] += px; v1[1] += py; v1[2] += pz;
        v2[0] -= px; v2[1] -= py; v2[2] -= pz;
        v3[0] -= px; v3[1] -= py; v3[2] -= pz;
        // filled whatever the polygon mode: in GL_LINE mode the quad's own edges were lines again, recursing until the stack
        // overflowed (CustomOreGen's wireframes crashed the game)
        emitFilled(st, s, v0, v1, v2, v3);
    }

    private static void emit(RenderContext.State st, Snapshot s, double[] a, double[] b, double[] c, double[] d) {
        // glPolygonMode(..., GL_LINE), per thread: filled primitives are drawn as their edges (CustomOreGen's wireframes)
        if (st.lineMode) {
            line(st, s, a, b);
            line(st, s, b, c);
            if (c != d) {
                line(st, s, c, d);
                line(st, s, d, a);
            } else {
                line(st, s, c, a);
            }
            return;
        }
        emitFilled(st, s, a, b, c, d);
    }

    private static void emitFilled(RenderContext.State st, Snapshot s, double[] a, double[] b, double[] c, double[] d) {
        float fx = s.nx(), fy = s.ny(), fz = s.nz();
        boolean gui = RenderContext.gui(st) != null;
        if (!s.hasNormal() && gui) {
            // GL's default current normal, facing the viewer: a face normal of a quad wound for y-down screen space points
            // away, and a screen drawn with GUI item lighting on came out black (the Thaumonomicon's frame)
            fx = 0.0F;
            fy = 0.0F;
            fz = 1.0F;
        } else if (!s.hasNormal()) {
            // Face normal from the first three corners.
            double ux = b[0] - a[0], uy = b[1] - a[1], uz = b[2] - a[2];
            double vx = c[0] - a[0], vy = c[1] - a[1], vz = c[2] - a[2];
            double cx = uy * vz - uz * vy, cy = uz * vx - ux * vz, cz = ux * vy - uy * vx;
            double len = Math.sqrt(cx * cx + cy * cy + cz * cz);
            if (len > 1.0E-9) {
                fx = (float) (cx / len);
                fy = (float) (cy / len);
                fz = (float) (cz / len);
            }
        }
        st.flatGuiQuad = !s.hasNormal() && gui;
        try {
            RenderContext.quad(st, a, b, c, d, fx, fy, fz);
        } finally {
            st.flatGuiQuad = false;
        }
    }
}
