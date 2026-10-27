package legacyconnector.v1_12_2.net.minecraftforge.fml.client.config;

import legacyconnector.v1_12_2.net.minecraft.client.Minecraft;
import org.kylepitcock.legacyconnector.v1_7_10.client.RenderContext;

/** FML's button with a scaled symbol before its text (the config screens' undo and reset buttons). */
public class GuiUnicodeGlyphButton extends GuiButtonExt {
    public String glyph;
    public float glyphScale;

    public GuiUnicodeGlyphButton(int id, int xPos, int yPos, int width, int height, String displayString, String glyph, float glyphScale) {
        super(id, xPos, yPos, width, height, displayString);
        this.glyph = glyph;
        this.glyphScale = glyphScale;
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY, float partial) {
        if (!visible) return;
        hovered = mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;
        mcc$drawBackground(getHoverState(hovered), x, y, width, height);
        mouseDragged(mc, mouseX, mouseY);
        int color = 14737632;
        if (packedFGColour != 0) color = packedFGColour;
        else if (!enabled) color = 10526880;
        else if (hovered) color = 16777120;
        String buttonText = displayString;
        int glyphWidth = (int) (mc.fontRenderer.getStringWidth(glyph) * glyphScale);
        int strWidth = mc.fontRenderer.getStringWidth(buttonText);
        int ellipsisWidth = mc.fontRenderer.getStringWidth("...");
        int totalWidth = strWidth + glyphWidth;
        if (totalWidth > width - 6 && totalWidth > ellipsisWidth) {
            buttonText = mc.fontRenderer.trimStringToWidth(buttonText, width - 6 - ellipsisWidth).trim() + "...";
        }
        strWidth = mc.fontRenderer.getStringWidth(buttonText);
        var gui = RenderContext.gui();
        if (gui != null) {
            gui.pose().pushPose();
            gui.pose().scale(glyphScale, glyphScale, 1.0F);
            drawCenteredString(mc.fontRenderer, glyph,
                    (int) ((x + width / 2 - strWidth / 2) / glyphScale - glyphWidth / (glyphScale * 2) + 2),
                    (int) ((y + (height - 8) / glyphScale / 2 - 1) / glyphScale), color);
            gui.pose().popPose();
        }
        drawCenteredString(mc.fontRenderer, buttonText, (int) (x + width / 2 + glyphWidth / glyphScale), y + (height - 8) / 2, color);
    }
}
