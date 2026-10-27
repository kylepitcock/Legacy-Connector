package legacyconnector.v1_12_2.net.minecraftforge.fml.client.config;

import legacyconnector.v1_12_2.net.minecraft.client.Minecraft;
import legacyconnector.v1_12_2.net.minecraft.client.gui.GuiButton;

/** FML's button of any size (the face is nine-sliced), whose text is cut with an ellipsis when it does not fit. */
public class GuiButtonExt extends GuiButton {
    public GuiButtonExt(int id, int xPos, int yPos, String displayString) {
        super(id, xPos, yPos, displayString);
    }

    public GuiButtonExt(int id, int xPos, int yPos, int width, int height, String displayString) {
        super(id, xPos, yPos, width, height, displayString);
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
        int strWidth = mc.fontRenderer.getStringWidth(buttonText);
        int ellipsisWidth = mc.fontRenderer.getStringWidth("...");
        if (strWidth > width - 6 && strWidth > ellipsisWidth) {
            buttonText = mc.fontRenderer.trimStringToWidth(buttonText, width - 6 - ellipsisWidth).trim() + "...";
        }
        drawCenteredString(mc.fontRenderer, buttonText, x + width / 2, y + (height - 8) / 2, color);
    }
}
