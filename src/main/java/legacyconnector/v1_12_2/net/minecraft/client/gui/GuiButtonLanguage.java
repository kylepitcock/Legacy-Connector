package legacyconnector.v1_12_2.net.minecraft.client.gui;

import legacyconnector.v1_12_2.net.minecraft.client.Minecraft;
import org.kylepitcock.legacyconnector.v1_7_10.client.RenderContext;

/** The 20x20 globe button of the title screen that opens the language list (the modern language icon on a button face). */
public class GuiButtonLanguage extends GuiButton {
    private static final net.minecraft.resources.ResourceLocation ICON = net.minecraft.resources.ResourceLocation.withDefaultNamespace("icon/language");

    public GuiButtonLanguage(int buttonID, int xPos, int yPos) {
        super(buttonID, xPos, yPos, 20, 20, "");
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
        if (!visible) return;
        hovered = mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;
        mcc$drawBackground(getHoverState(hovered), x, y, width, height);
        var gui = RenderContext.gui();
        if (gui != null) gui.blitSprite(ICON, x + (width - 15) / 2, y + (height - 15) / 2, 15, 15);
    }
}
