package legacyconnector.v1_7_10.net.minecraft.client.renderer;

import org.kylepitcock.legacyconnector.v1_7_10.client.DisplayLists;
import org.kylepitcock.legacyconnector.v1_7_10.client.LegacyVertices;

/** 1.7.10 Tessellator: buffered legacy geometry, emitted into the active render context on draw(). */
public class Tessellator {
    public static final Tessellator instance = new Tessellator();

    public boolean isDrawing;
    /**
     * 1.7.10's fields of the same names: the current vertex colour (ABGR, as the real Tessellator packed it) and brightness. Mods read them
     * by reflection to save and restore the state (Applied Energistics 2's RenderBlocksWorkaround keeps the colour of each face it lights
     * and sets it again with setColorRGBA_I for every part drawn from the cached lighting: a failed read gave 0, which drew every cable
     * bus part black).
     */
    public int color = 0xFFFFFFFF;
    public int brightness;
    private final LegacyVertices vertices = new LegacyVertices();

    public int draw() {
        if (!isDrawing) throw new IllegalStateException("Not tesselating!");
        isDrawing = false;
        if (DisplayLists.recording()) {
            // compiling a display list: the draw is kept and replayed by glCallList (also run now when compiling and executing)
            var draw = vertices.take();
            if (!DisplayLists.record(() -> LegacyVertices.emit(draw))) LegacyVertices.emit(draw);
            return draw.vertices().size();
        }
        return vertices.end();
    }

    /** Leaves a draw that a failed legacy renderer began and never ended, so the next renderer does not hit "Already tesselating!". */
    public void mcc$reset() {
        if (!isDrawing) return;
        isDrawing = false;
        vertices.cancel();
    }

    public void startDrawingQuads() {
        startDrawing(LegacyVertices.GL_QUADS);
    }

    public void startDrawing(int mode) {
        if (isDrawing) throw new IllegalStateException("Already tesselating!");
        isDrawing = true;
        vertices.begin(mode);
    }

    public void setTextureUV(double u, double v) {
        vertices.uv(u, v);
    }

    public void setBrightness(int brightness) {
        this.brightness = brightness;
        vertices.light(brightness);
    }

    public void setColorOpaque_F(float r, float g, float b) {
        setColorRGBA_F(r, g, b, 1.0F);
    }

    public void setColorRGBA_F(float r, float g, float b, float a) {
        vertices.color(r, g, b, a);
        note(to255(r), to255(g), to255(b), to255(a));
    }

    public void setColorOpaque(int r, int g, int b) {
        setColorRGBA(r, g, b, 255);
    }

    public void setColorRGBA(int r, int g, int b, int a) {
        vertices.color(r, g, b, a);
        note(r, g, b, a);
    }

    private static int to255(float f) {
        return Math.max(0, Math.min(255, (int) (f * 255.0F)));
    }

    private void note(int r, int g, int b, int a) {
        color = (a & 255) << 24 | (b & 255) << 16 | (g & 255) << 8 | (r & 255);
    }

    /**
     * Records the colour 1.7.10's renderer would have set for an untinted face (its shade) in {@link #color} without making it the vertex colour:
     * the emulation draws such faces uncoloured so the modern pipeline shades them, while a mod that reads the field back sees what it saw in 1.7.10.
     */
    public void mcc$noteFaceColor(float shade) {
        int c = to255(shade);
        note(c, c, c, 255);
    }

    public void setColorOpaque_I(int rgb) {
        setColorOpaque(rgb >> 16 & 255, rgb >> 8 & 255, rgb & 255);
    }

    public void setColorRGBA_I(int rgb, int alpha) {
        setColorRGBA(rgb >> 16 & 255, rgb >> 8 & 255, rgb & 255, alpha);
    }

    /** The explicit vertex colour (ARGB), or -1 when none is set (the emulation then lets the modern pipeline shade). */
    public long mcc$colorState() {
        return vertices.colorState();
    }

    public void mcc$setColorState(long state) {
        vertices.setColorState(state);
        if (state >= 0) note((int) (state >> 16) & 255, (int) (state >> 8) & 255, (int) state & 255, (int) (state >> 24) & 255);
    }

    public void disableColor() {
        vertices.disableColor();
    }

    public void setNormal(float x, float y, float z) {
        vertices.normal(x, y, z);
    }

    public void setTranslation(double x, double y, double z) {
        vertices.translation(x, y, z);
    }

    public void addTranslation(float x, float y, float z) {
        vertices.addTranslation(x, y, z);
    }

    public void addVertexWithUV(double x, double y, double z, double u, double v) {
        setTextureUV(u, v);
        addVertex(x, y, z);
    }

    public void addVertex(double x, double y, double z) {
        vertices.vertex(x, y, z);
    }

    /** Forge: set while the chunk renderer draws world geometry (mods check it to tell world from item rendering). */
    public static boolean renderingWorldRenderer = false;
    /** Forge: whether the draw uses the default (block atlas) texture. */
    public boolean defaultTexture = false;

    public void func_154352_a(byte r, byte g, byte b) {
        setColorOpaque(r & 255, g & 255, b & 255);
    }

    /**
     * Sorts the quads drawn so far back to front as seen from (x, y, z) (translucent geometry, as 1.7.10's chunk renderer did) and
     * returns them as a vertex state in 1.7.10's raw format: 8 ints per vertex (x, y, z, u, v as float bits, colour ABGR, light,
     * normal).
     */
    public legacyconnector.v1_7_10.net.minecraft.client.shader.TesselatorVertexState getVertexState(float x, float y, float z) {
        java.util.List<double[]> list = vertices.mcc$vertices();
        int quads = list.size() / 4;
        double cx = x + vertices.mcc$tx(), cy = y + vertices.mcc$ty(), cz = z + vertices.mcc$tz();
        Integer[] order = new Integer[quads];
        double[] dist = new double[quads];
        for (int q = 0; q < quads; q++) {
            order[q] = q;
            double mx = 0, my = 0, mz = 0;
            for (int i = 0; i < 4; i++) {
                double[] v = list.get(q * 4 + i);
                mx += v[0] * 0.25D;
                my += v[1] * 0.25D;
                mz += v[2] * 0.25D;
            }
            mx -= cx;
            my -= cy;
            mz -= cz;
            dist[q] = mx * mx + my * my + mz * mz;
        }
        java.util.Arrays.sort(order, (a, b) -> Double.compare(dist[b], dist[a]));
        java.util.List<double[]> sorted = new java.util.ArrayList<>(list.size());
        for (int q = 0; q < quads; q++) {
            for (int i = 0; i < 4; i++) sorted.add(list.get(order[q] * 4 + i));
        }
        for (int i = quads * 4; i < list.size(); i++) sorted.add(list.get(i));
        list.clear();
        list.addAll(sorted);
        int[] raw = new int[list.size() * 8];
        boolean color = false;
        for (int i = 0; i < list.size(); i++) {
            double[] v = list.get(i);
            int o = i * 8;
            raw[o] = Float.floatToRawIntBits((float) v[0]);
            raw[o + 1] = Float.floatToRawIntBits((float) v[1]);
            raw[o + 2] = Float.floatToRawIntBits((float) v[2]);
            raw[o + 3] = Float.floatToRawIntBits((float) v[3]);
            raw[o + 4] = Float.floatToRawIntBits((float) v[4]);
            int argb = (int) (long) v[5];
            raw[o + 5] = argb & 0xFF00FF00 | (argb & 255) << 16 | (argb >> 16) & 255;
            raw[o + 6] = (int) v[6];
            color |= v.length > 7 && v[7] != 0.0D;
        }
        var state = new legacyconnector.v1_7_10.net.minecraft.client.shader.TesselatorVertexState(raw, raw.length, list.size(), true, true, false, color);
        state.mcc$vertices = new java.util.ArrayList<>(list);
        return state;
    }

    /** Replaces the vertices of the current draw with a saved state (one of {@link #getVertexState}, or built from raw data). */
    public void setVertexState(legacyconnector.v1_7_10.net.minecraft.client.shader.TesselatorVertexState state) {
        java.util.List<double[]> list = vertices.mcc$vertices();
        list.clear();
        if (state.mcc$vertices != null) {
            list.addAll(state.mcc$vertices);
            return;
        }
        int[] raw = state.getRawBuffer();
        int n = Math.min(state.getVertexCount(), raw.length / 8);
        for (int i = 0; i < n; i++) {
            int o = i * 8;
            int abgr = raw[o + 5];
            long argb = (abgr & 0xFF00FF00 | (abgr & 255) << 16 | (abgr >> 16) & 255) & 0xFFFFFFFFL;
            list.add(new double[] {Float.intBitsToFloat(raw[o]), Float.intBitsToFloat(raw[o + 1]), Float.intBitsToFloat(raw[o + 2]),
                    Float.intBitsToFloat(raw[o + 3]), Float.intBitsToFloat(raw[o + 4]), argb, state.getHasBrightness() ? raw[o + 6] : -1,
                    state.getHasColor() ? 1.0D : 0.0D});
        }
    }
}
