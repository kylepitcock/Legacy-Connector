package legacyconnector.vb1_7_3.net.minecraft.client.font;

import org.kylepitcock.legacyconnector.vb1_7_3.GuiDrawB173;
import org.kylepitcock.legacyconnector.vb1_7_3.RendererContextB173;

/** Beta 1.7.3 {@code FontRenderer}: drawn with the modern font ({@link GuiDrawB173}); § formatting codes still apply. */
public class TextRenderer {
    public int boundTexture;

    protected TextRenderer() {}

    private static final TextRenderer INSTANCE = new TextRenderer();

    public static TextRenderer mcc$instance() {
        return INSTANCE;
    }

    public void drawWithShadow(String text, int x, int y, int color) {
        draw(text,x,y,color,true);
    }

    public void draw(String text, int x, int y, int color) {
        draw(text,x,y,color,false);
    }

    public void draw(String text, int x, int y, int color, boolean shadow) {
        if(RendererContextB173.active())RendererContextB173.text(text,x,y,color,shadow);else GuiDrawB173.text(text, x, y, color, shadow);
    }

    public int getWidth(String text) {
        return RendererContextB173.active()?RendererContextB173.width(text):GuiDrawB173.width(text);
    }

    /** MCP {@code drawSplitString}: word-wrapped at {@code width}. */
    public void drawSplit(String text, int x, int y, int width, int color) {
        for (String line : wrap(text, width)) {
            draw(line, x, y, color);
            y += 8;
        }
    }

    public int splitAndGetHeight(String text, int width) {
        return wrap(text, width).size() * 8;
    }

    private java.util.List<String> wrap(String text, int width) {
        java.util.List<String> lines = new java.util.ArrayList<>();
        for (String paragraph : text.split("\n")) {
            StringBuilder line = new StringBuilder();
            for (String word : paragraph.split(" ")) {
                String next = line.length() == 0 ? word : line + " " + word;
                if (line.length() > 0 && getWidth(next) > width) {
                    lines.add(line.toString());
                    line = new StringBuilder(word);
                } else {
                    line = new StringBuilder(next);
                }
            }
            lines.add(line.toString());
        }
        return lines;
    }
}
