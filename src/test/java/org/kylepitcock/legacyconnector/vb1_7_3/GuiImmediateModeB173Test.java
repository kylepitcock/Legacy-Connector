package org.kylepitcock.legacyconnector.vb1_7_3;

import static org.junit.jupiter.api.Assertions.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import legacyconnector.vb1_7_3.net.minecraft.client.render.Tessellator;
import legacyconnector.vb1_7_3.org.lwjgl.opengl.GL11;
import org.junit.jupiter.api.Test;

/** Legacy GUIs that draw with Tessellator quads and orthographic GL (a loading screen) reach the screen sink. */
class GuiImmediateModeB173Test {
    private static final class Recorder implements GuiDrawB173.Sink {
        final List<String> calls = new ArrayList<>();
        final List<double[]> xs = new ArrayList<>(), ys = new ArrayList<>(), us = new ArrayList<>(), vs = new ArrayList<>();
        final List<int[]> colours = new ArrayList<>();
        @Override public boolean drawing() {return true;}
        @Override public void quad(double[] x, double[] y, double[] u, double[] v, int[] argb) {calls.add("quad");xs.add(x);ys.add(y);us.add(u);vs.add(v);colours.add(argb);}
        @Override public void texture2d(boolean enabled) {calls.add("texture2d " + enabled);}
        @Override public void matrixMode(int mode) {calls.add("mode " + mode);}
        @Override public void loadIdentity() {calls.add("identity");}
        @Override public void ortho(double l, double r, double b, double t) {calls.add("ortho " + l + " " + r + " " + b + " " + t);}
        @Override public void blit(int x, int y, int u, int v, int width, int height) {}
        @Override public void fill(int x1, int y1, int x2, int y2, int color) {}
        @Override public void fillGradient(int x1, int y1, int x2, int y2, int top, int bottom) {}
        @Override public void text(String text, int x, int y, int color, boolean shadow) {}
        @Override public int width(String text) {return 0;}
        @Override public int textureId(String path) {return 1;}
        @Override public int load(BufferedImage image) {return 1;}
        @Override public void bind(int id) {}
        @Override public void color(float r, float g, float b, float a) {}
        @Override public void push() {}
        @Override public void pop() {}
        @Override public void translate(float x, float y, float z) {}
        @Override public void scale(float x, float y, float z) {}
        @Override public void click() {}
    }

    @Test void tessellatorQuadsReachTheGuiWithArgbColoursAndTextureCoordinates() {
        var sink = new Recorder();
        GuiDrawB173.withSink(sink, () -> {
            var tessellator = Tessellator.INSTANCE;
            tessellator.startQuads();
            tessellator.color(0x404040);
            tessellator.vertex(0, 20, 0, 0, 20 / 32.0);
            tessellator.vertex(40, 20, 0, 40 / 32.0, 20 / 32.0);
            tessellator.vertex(40, 0, 0, 40 / 32.0, 0);
            tessellator.vertex(0, 0, 0, 0, 0);
            tessellator.draw();
        });
        assertEquals(List.of("quad"), sink.calls);
        assertArrayEquals(new double[] {0, 40, 40, 0}, sink.xs.get(0));
        assertArrayEquals(new double[] {20, 20, 0, 0}, sink.ys.get(0));
        assertEquals(40 / 32.0, sink.us.get(0)[1], 1e-9);
        for (int c : sink.colours.get(0)) assertEquals(0xFF404040, c);
    }

    @Test void guiGlCallsReachTheSinkAndAModelRendererContextIsNotDisturbed() {
        var sink = new Recorder();
        GuiDrawB173.withSink(sink, () -> {
            GL11.glMatrixMode(5889);
            GL11.glLoadIdentity();
            GL11.glOrtho(0, 427, 240, 0, 100, 300);
            GL11.glMatrixMode(5888);
            GL11.glLoadIdentity();
            GL11.glDisable(3553);
            GL11.glEnable(3553);
            GL11.glDisable(2896);
        });
        assertEquals(List.of("mode 5889", "identity", "ortho 0.0 427.0 240.0 0.0", "mode 5888", "identity", "texture2d false", "texture2d true"), sink.calls);
    }

    @Test void withoutAGuiSinkTessellatorIsInert() {
        var tessellator = Tessellator.INSTANCE;
        tessellator.startQuads();
        tessellator.vertex(0, 0, 0, 0, 0);
        tessellator.draw();
        assertDoesNotThrow(() -> GL11.glOrtho(0, 1, 1, 0, 0, 1));
    }
}
