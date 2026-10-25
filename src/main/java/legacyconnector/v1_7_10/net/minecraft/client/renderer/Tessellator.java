package legacyconnector.v1_7_10.net.minecraft.client.renderer;

import org.kylepitcock.legacyconnector.v1_7_10.client.DisplayLists;
import org.kylepitcock.legacyconnector.v1_7_10.client.LegacyVertices;

/** 1.7.10 Tessellator: buffered legacy geometry, emitted into the active render context on draw(). */
public class Tessellator {
    public static final Tessellator instance = new Tessellator();

    public boolean isDrawing;
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
        vertices.light(brightness);
    }

    public void setColorOpaque_F(float r, float g, float b) {
        vertices.color(r, g, b, 1.0F);
    }

    public void setColorRGBA_F(float r, float g, float b, float a) {
        vertices.color(r, g, b, a);
    }

    public void setColorOpaque(int r, int g, int b) {
        vertices.color(r, g, b, 255);
    }

    public void setColorRGBA(int r, int g, int b, int a) {
        vertices.color(r, g, b, a);
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
}
