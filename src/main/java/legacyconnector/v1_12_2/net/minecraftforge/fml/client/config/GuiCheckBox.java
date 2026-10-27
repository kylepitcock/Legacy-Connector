package legacyconnector.v1_12_2.net.minecraftforge.fml.client.config;

import legacyconnector.v1_12_2.net.minecraft.client.Minecraft;
import legacyconnector.v1_12_2.net.minecraft.client.gui.GuiButton;

/** FML's check box: an 11x11 box with an "x" when checked and the label to its right; clicking toggles it. */
public class GuiCheckBox extends GuiButton {
    private boolean isChecked;
    private final int boxWidth;

    public GuiCheckBox(int id, int xPos, int yPos, String displayString, boolean isChecked) {
        super(id, xPos, yPos, displayString);
        this.isChecked = isChecked;
        this.boxWidth = 11;
        this.height = 11;
        this.width = this.boxWidth + 2 + Minecraft.getMinecraft().fontRenderer.getStringWidth(displayString);
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY, float partial) {
        if (!visible) return;
        hovered = mouseX >= x && mouseY >= y && mouseX < x + boxWidth && mouseY < y + height;
        GuiUtils.drawContinuousTexturedBox(BUTTON_TEXTURES, x, y, 0, 46, boxWidth, height, 200, 20, 2, 3, 2, 2, zLevel);
        mouseDragged(mc, mouseX, mouseY);
        int color = 14737632;
        if (packedFGColour != 0) color = packedFGColour;
        else if (!enabled) color = 10526880;
        if (isChecked) drawCenteredString(mc.fontRenderer, "x", x + boxWidth / 2 + 1, y + 1, 14737632);
        drawString(mc.fontRenderer, displayString, x + boxWidth + 2, y + 2, color);
    }

    @Override
    public boolean mousePressed(Minecraft mc, int mouseX, int mouseY) {
        if (enabled && visible && mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height) {
            isChecked = !isChecked;
            return true;
        }
        return false;
    }

    public boolean isChecked() {
        return isChecked;
    }

    public void setIsChecked(boolean isChecked) {
        this.isChecked = isChecked;
    }
}
