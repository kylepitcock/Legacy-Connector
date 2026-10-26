package legacyconnector.v1_7_10.net.minecraft.client.gui;

import java.util.ArrayList;
import legacyconnector.v1_7_10.net.minecraft.client.Minecraft;

/** 1.7.10's text label of a screen's {@code labelList}: lines in a box, optionally centred and with a bordered background. */
public class GuiLabel extends Gui {
    protected int field_146167_a = 200;
    protected int field_146161_f = 20;
    public int field_146162_g;
    public int field_146174_h;
    protected ArrayList field_146173_k = new ArrayList();
    protected boolean centered;
    public boolean visible = true;
    protected boolean labelBgEnabled;
    protected int field_146168_n;
    protected int field_146169_o;
    protected int field_146166_p;
    protected int field_146165_q;
    protected FontRenderer fontRenderer;
    protected int field_146163_s;

    public void drawLabel(Minecraft mc, int mouseX, int mouseY) {
        if (!visible) return;
        drawLabelBackground(mc, mouseX, mouseY);
        FontRenderer font = fontRenderer != null ? fontRenderer : mc.fontRenderer;
        int middle = field_146174_h + field_146161_f / 2 + field_146163_s / 2;
        int top = middle - field_146173_k.size() * 10 / 2;
        for (int i = 0; i < field_146173_k.size(); i++) {
            String line = String.valueOf(field_146173_k.get(i));
            if (centered) drawCenteredString(font, line, field_146162_g + field_146167_a / 2, top + i * 10, field_146168_n);
            else drawString(font, line, field_146162_g, top + i * 10, field_146168_n);
        }
    }

    protected void drawLabelBackground(Minecraft mc, int mouseX, int mouseY) {
        if (!labelBgEnabled) return;
        int w = field_146167_a + field_146163_s * 2;
        int h = field_146161_f + field_146163_s * 2;
        int x = field_146162_g - field_146163_s;
        int y = field_146174_h - field_146163_s;
        drawRect(x, y, x + w, y + h, field_146169_o);
        drawHorizontalLine(x, x + w, y, field_146166_p);
        drawHorizontalLine(x, x + w, y + h, field_146165_q);
        drawVerticalLine(x, y, y + h, field_146166_p);
        drawVerticalLine(x + w, y, y + h, field_146165_q);
    }
}
