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
        vertices.add(new double[] {x + tx, y + ty, z + tz, u, v, color & 0xFFFFFFFFL, light >= 0 ? light : RenderContext.vertexLight()});
    }

    /** Emits the assembled primitives; returns the vertex count like the legacy Tessellator. */
    public int end() {
        drawing = false;
        int n = vertices.size();
        switch (mode) {
            case GL_QUADS -> {
                for (int i = 0; i + 3 < n; i += 4) emit(vertices.get(i), vertices.get(i + 1), vertices.get(i + 2), vertices.get(i + 3));
            }
            case GL_TRIANGLES -> {
                for (int i = 0; i + 2 < n; i += 3) emit(vertices.get(i), vertices.get(i + 1), vertices.get(i + 2), vertices.get(i + 2));
            }
            case GL_TRIANGLE_FAN, GL_POLYGON -> {
                for (int i = 1; i + 1 < n; i++) emit(vertices.get(0), vertices.get(i), vertices.get(i + 1), vertices.get(i + 1));
            }
            case GL_TRIANGLE_STRIP -> {
                for (int i = 0; i + 2 < n; i++) {
                    if ((i & 1) == 0) emit(vertices.get(i), vertices.get(i + 1), vertices.get(i + 2), vertices.get(i + 2));
                    else emit(vertices.get(i + 1), vertices.get(i), vertices.get(i + 2), vertices.get(i + 2));
                }
            }
            case GL_QUAD_STRIP -> {
                for (int i = 0; i + 3 < n; i += 2) emit(vertices.get(i), vertices.get(i + 1), vertices.get(i + 3), vertices.get(i + 2));
            }
            default -> {
                // lines and points have no counterpart in the entity pipelines
            }
        }
        vertices.clear();
        return n;
    }

    private void emit(double[] a, double[] b, double[] c, double[] d) {
        float fx = nx, fy = ny, fz = nz;
        if (!hasNormal) {
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
