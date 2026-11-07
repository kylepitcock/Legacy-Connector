package org.kylepitcock.legacyconnector.vb1_7_3;

import java.awt.image.BufferedImage;

/**
 * Where b1.7.3 GUI drawing goes ({@code Gui.drawTexturedModalRect}, {@code FontRenderer.drawString}, {@code RenderEngine.getTexture},
 * the GL11 state calls GUIs make). The shim's GUI classes call these static methods only, with plain types, so they load on a dedicated
 * server too; the client installs a {@link Sink} drawing into the screen being rendered. Without one (server, or outside rendering)
 * everything is a no-op.
 */
public final class GuiDrawB173 {
    /** The client's drawing, while a legacy GUI renders. */
    public interface Sink {
        void blit(int x, int y, int u, int v, int width, int height);

        void fill(int x1, int y1, int x2, int y2, int color);

        void fillGradient(int x1, int y1, int x2, int y2, int top, int bottom);

        void text(String text, int x, int y, int color, boolean shadow);

        int width(String text);

        int textureId(String path);

        int load(BufferedImage image);

        void bind(int id);

        void color(float r, float g, float b, float a);

        void push();

        void pop();

        void translate(float x, float y, float z);

        void scale(float x, float y, float z);

        /** A button's click sound. */
        void click();
    }

    private static volatile Sink sink;

    private GuiDrawB173() {}

    public static void install(Sink s) {
        sink = s;
    }

    public static void blit(int x, int y, int u, int v, int width, int height) {
        Sink s = sink;
        if (s != null) s.blit(x, y, u, v, width, height);
    }

    public static void fill(int x1, int y1, int x2, int y2, int color) {
        Sink s = sink;
        if (s != null) s.fill(x1, y1, x2, y2, color);
    }

    public static void fillGradient(int x1, int y1, int x2, int y2, int top, int bottom) {
        Sink s = sink;
        if (s != null) s.fillGradient(x1, y1, x2, y2, top, bottom);
    }

    public static void text(String text, int x, int y, int color, boolean shadow) {
        Sink s = sink;
        if (s != null && text != null) s.text(text, x, y, color, shadow);
    }

    public static int width(String text) {
        Sink s = sink;
        return s != null && text != null ? s.width(text) : text == null ? 0 : text.length() * 6;
    }

    public static int textureId(String path) {
        Sink s = sink;
        return s != null ? s.textureId(path) : 0;
    }

    public static int load(BufferedImage image) {
        Sink s = sink;
        return s != null ? s.load(image) : 0;
    }

    public static void bind(int id) {
        Sink s = sink;
        if (s != null) s.bind(id);
    }

    public static void color(float r, float g, float b, float a) {
        Sink s = sink;
        if (s != null) s.color(r, g, b, a);
    }

    public static void push() {
        Sink s = sink;
        if (s != null) s.push();
    }

    public static void pop() {
        Sink s = sink;
        if (s != null) s.pop();
    }

    public static void translate(float x, float y, float z) {
        Sink s = sink;
        if (s != null) s.translate(x, y, z);
    }

    public static void scale(float x, float y, float z) {
        Sink s = sink;
        if (s != null) s.scale(x, y, z);
    }

    public static void click() {
        Sink s = sink;
        if (s != null) s.click();
    }
}
