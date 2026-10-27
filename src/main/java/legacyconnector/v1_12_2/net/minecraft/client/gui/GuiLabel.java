package legacyconnector.v1_12_2.net.minecraft.client.gui;

import java.util.ArrayList;
import java.util.List;
import legacyconnector.v1_12_2.net.minecraft.client.Minecraft;

/** A block of translated text lines a screen draws (optionally centred, optionally on a coloured box). */
public class GuiLabel extends Gui {
    protected int width = 200;
    protected int height = 20;
    public int x;
    public int y;
    private final List<String> labels = new ArrayList<>();
    public int id;
    private boolean centered;
    public boolean visible = true;
    private boolean labelBgEnabled;
    private final int textColor;
    private int backColor;
    private int ulColor;
    private int brColor;
    private final FontRenderer fontRenderer;
    private int border;

    public GuiLabel(FontRenderer fontRenderer, int id, int x, int y, int width, int height, int color) {
        this.fontRenderer = fontRenderer != null ? fontRenderer : FontRenderer.mcc$INSTANCE;
        this.id = id;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.textColor = color;
    }

    public void addLine(String line) {
        labels.add(legacyconnector.v1_12_2.net.minecraft.client.resources.I18n.format(line));
    }

    public GuiLabel setCentered() {
        this.centered = true;
        return this;
    }

    public void drawLabel(Minecraft mc, int mouseX, int mouseY) {
        if (!visible) return;
        drawLabelBackground(mc, mouseX, mouseY);
        int i = y + height / 2 + border / 2;
        int j = i - labels.size() * 10 / 2;
        for (int k = 0; k < labels.size(); ++k) {
            if (centered) drawCenteredString(fontRenderer, labels.get(k), x + width / 2, j + k * 10, textColor);
            else drawString(fontRenderer, labels.get(k), x, j + k * 10, textColor);
        }
    }

    protected void drawLabelBackground(Minecraft mc, int mouseX, int mouseY) {
        if (!labelBgEnabled) return;
        int i = width + border * 2;
        int j = height + border * 2;
        int k = x - border;
        int l = y - border;
        drawRect(k, l, k + i, l + j, backColor);
        drawHorizontalLine(k, k + i, l, ulColor);
        drawHorizontalLine(k, k + i, l + j, brColor);
        drawVerticalLine(k, l, l + j, ulColor);
        drawVerticalLine(k + i, l, l + j, brColor);
    }
}
