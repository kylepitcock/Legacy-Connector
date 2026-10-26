package org.kylepitcock.legacyconnector.v1_7_10.client;

import java.util.ArrayList;
import java.util.List;

/**
 * Assembles legacy immediate-mode geometry (Tessellator, glBegin/glEnd) into quads for {@link RenderContext}:
 * quads as they are, triangles/fans/strips as degenerate quads. Lines and points are dropped.
 */
public final class LegacyVertices {
    public static final int GL_POINTS = 0, GL_LINES = 1, GL_LINE_LOOP = 2, GL_LINE_STRIP = 3, GL_TRIANGLES = 4,
            GL_TRIANGLE_STRIP = 5, GL_TRIANGLE_FAN = 6, GL_QUADS = 7, GL_QUAD_STRIP = 8, GL_POLYGON = 9;

    private final List<double[]> vertices = new ArrayList<>();
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
        vertices.clear();
        hasColor = false;
        hasNormal = false;
        colorDisabled = false;
        light = -1;
    }

    /** Drops geometry begun but never ended (a legacy renderer that threw midway). */
    public void cancel() {
        drawing = false;
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
        int color = hasColor ? argb : RenderContext.argb();
        // [7]: the vertex carries its own colour; otherwise it takes the colour current when the geometry is emitted (display lists replay later)
        vertices.add(new double[] {x + tx, y + ty, z + tz, u, v, color & 0xFFFFFFFFL, light >= 0 ? light : RenderContext.vertexLight(), hasColor ? 1.0D : 0.0D});
    }

    /** A finished draw: what {@link #emit} needs to produce its quads later (display lists keep these). */
    public record Snapshot(int mode, List<double[]> vertices, float nx, float ny, float nz, boolean hasNormal) {}

    /** Ends the draw without emitting it. */
    public Snapshot take() {
        drawing = false;
        Snapshot s = new Snapshot(mode, new ArrayList<>(vertices), nx, ny, nz, hasNormal);
        vertices.clear();
        return s;
    }

    /** Emits the assembled primitives; returns the vertex count like the legacy Tessellator. */
    public int end() {
        Snapshot s = take();
        emit(s);
        return s.vertices().size();
    }

    public static void emit(Snapshot s) {
        List<double[]> vertices = s.vertices();
        int n = vertices.size();
        long current = RenderContext.argb() & 0xFFFFFFFFL;
        for (int i = 0; i < n; i++) {
            double[] v = vertices.get(i);
            if (v.length > 7 && v[7] == 0.0D && (long) v[5] != current) {
                v = v.clone();
                v[5] = current;
                vertices.set(i, v);
            }
        }
        switch (s.mode()) {
            case GL_QUADS -> {
                for (int i = 0; i + 3 < n; i += 4) emit(s, vertices.get(i), vertices.get(i + 1), vertices.get(i + 2), vertices.get(i + 3));
            }
            case GL_TRIANGLES -> {
                for (int i = 0; i + 2 < n; i += 3) emit(s, vertices.get(i), vertices.get(i + 1), vertices.get(i + 2), vertices.get(i + 2));
            }
            case GL_TRIANGLE_FAN, GL_POLYGON -> {
                for (int i = 1; i + 1 < n; i++) emit(s, vertices.get(0), vertices.get(i), vertices.get(i + 1), vertices.get(i + 1));
            }
            case GL_TRIANGLE_STRIP -> {
                for (int i = 0; i + 2 < n; i++) {
                    if ((i & 1) == 0) emit(s, vertices.get(i), vertices.get(i + 1), vertices.get(i + 2), vertices.get(i + 2));
                    else emit(s, vertices.get(i + 1), vertices.get(i), vertices.get(i + 2), vertices.get(i + 2));
                }
            }
            case GL_QUAD_STRIP -> {
                for (int i = 0; i + 3 < n; i += 2) emit(s, vertices.get(i), vertices.get(i + 1), vertices.get(i + 3), vertices.get(i + 2));
            }
            case GL_LINES -> {
                for (int i = 0; i + 1 < n; i += 2) line(s, vertices.get(i), vertices.get(i + 1));
            }
            case GL_LINE_STRIP -> {
                for (int i = 0; i + 1 < n; i++) line(s, vertices.get(i), vertices.get(i + 1));
            }
            case GL_LINE_LOOP -> {
                for (int i = 0; i < n && n > 1; i++) line(s, vertices.get(i), vertices.get((i + 1) % n));
            }
            default -> {
                // points have no counterpart in the entity pipelines
            }
        }
        // 1.7.10 drew immediately: in a screen, what comes later (blitted textures, items) must cover this draw, so it is
        // not left batched until the end of the frame (Thaumonomicon lines showed over the research icons)
        var gui = RenderContext.gui();
        if (gui != null) {
            // the vertices already carry glColor (or their own colour, which in 1.7.10 replaced it): the shader colour the
            // screen applies would multiply it in a second time (Thaumcraft's aspects came out dark)
            float[] shader = com.mojang.blaze3d.systems.RenderSystem.getShaderColor().clone();
            com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            try {
                gui.flush();
            } finally {
                com.mojang.blaze3d.systems.RenderSystem.setShaderColor(shader[0], shader[1], shader[2], shader[3]);
            }
        }
    }

    /**
     * A line is a thin quad (the pipelines draw no lines): one GUI unit wide in screens, a hair wide in the world; it lies in the xy plane
     * when the segment does, and otherwise is widened sideways.
     */
    private static void line(Snapshot s, double[] a, double[] b) {
        double dx = b[0] - a[0], dy = b[1] - a[1], dz = b[2] - a[2];
        double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 1.0E-9) return;
        double half = RenderContext.gui() != null ? 0.5D : 0.01D;
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
        emit(s, v0, v1, v2, v3);
    }

    private static void emit(Snapshot s, double[] a, double[] b, double[] c, double[] d) {
        float fx = s.nx(), fy = s.ny(), fz = s.nz();
        if (!s.hasNormal()) {
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
        RenderContext.quad(new double[][] {a, b, c, d}, fx, fy, fz);
    }
}
